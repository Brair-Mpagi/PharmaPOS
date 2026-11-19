package com.pharmapos.cloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates a cryptographically random, per-request Content-Security-Policy nonce
 * and sets it on the response for use by the back-office server-rendered pages
 * (TDD §9.1 "Back-office session", §9.5 "Browser hardening").
 *
 * <h3>What it does:</h3>
 * <ol>
 *   <li>Generates a 128-bit random nonce, Base64url-encoded.</li>
 *   <li>Stores the nonce in a request attribute ({@value #NONCE_ATTRIBUTE}) so that
 *       Thymeleaf / Next.js SSR can embed it in {@code <script nonce="...">} tags.</li>
 *   <li>Sets the {@code Content-Security-Policy} response header with the nonce,
 *       blocking all inline scripts that do not carry the nonce.</li>
 * </ol>
 *
 * <p>The filter runs before all business filters (order {@link Ordered#HIGHEST_PRECEDENCE} + 5)
 * so the CSP header is always set, even when a downstream filter returns early.
 *
 * <p>The CSP policy follows TDD §9.5:
 * <ul>
 *   <li>No inline scripts without the per-request nonce</li>
 *   <li>No {@code eval()} (enforced by {@code unsafe-eval} absent)</li>
 *   <li>Frame ancestors denied</li>
 *   <li>Subresource integrity on third-party scripts is enforced by the
 *       front-end build; this header does not use {@code require-sri-for}
 *       (too strict for CDN scenarios in older browsers)</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CspNonceFilter extends OncePerRequestFilter {

    /** Request attribute name for downstream template engines to pick up the nonce. */
    public static final String NONCE_ATTRIBUTE = "cspNonce";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        String nonce = generateNonce();
        request.setAttribute(NONCE_ATTRIBUTE, nonce);

        // Build the CSP header. The nonce must be included in every <script> and <style> tag.
        String csp = buildCsp(nonce, request.getRequestURI());
        response.setHeader("Content-Security-Policy", csp);

        // Additional security headers (belt-and-suspenders; also set in SecurityConfig)
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy",
            "camera=(), microphone=(), geolocation=(), payment=()");

        chain.doFilter(request, response);
    }

    /**
     * Generates a 128-bit (16 byte) random nonce, Base64url-encoded without padding.
     */
    private static String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Builds the CSP header value. API paths get a minimal policy (no HTML);
     * back-office and platform paths get the full policy with nonce.
     */
    private static String buildCsp(String nonce, String uri) {
        boolean isApiPath = uri.startsWith("/api/") || uri.startsWith("/sync/");

        if (isApiPath) {
            // JSON API responses do not need a script-src policy
            return "default-src 'none'; frame-ancestors 'none'";
        }

        // Full browser-facing CSP with per-request nonce
        return String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'nonce-" + nonce + "'",
            "style-src 'self' 'nonce-" + nonce + "'",
            "img-src 'self' data: blob:",
            "font-src 'self'",
            "connect-src 'self'",
            "frame-ancestors 'none'",
            "form-action 'self'",
            "base-uri 'self'",
            "object-src 'none'",
            "upgrade-insecure-requests"
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Apply to all requests — the per-path logic inside the filter handles minimisation
        return false;
    }
}
