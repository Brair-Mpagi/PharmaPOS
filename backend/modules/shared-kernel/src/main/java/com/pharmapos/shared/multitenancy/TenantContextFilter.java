package com.pharmapos.shared.multitenancy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servlet filter that authenticates every request via JWT, enforces the
 * tenant/subdomain binding, and builds an immutable {@link TenantContext}
 * for the duration of the request (TDD §5.1).
 *
 * <h3>Security checks performed in order:</h3>
 * <ol>
 *   <li>JWT present in {@code Authorization: Bearer ...} header</li>
 *   <li>Signature valid — verified against the Keycloak JWKS endpoint</li>
 *   <li>Not expired</li>
 *   <li>Audience claim contains the expected value</li>
 *   <li>{@code tenant_id} claim matches the host subdomain — mismatch → 403 (TDD §5.1 step 2)</li>
 * </ol>
 *
 * <p>The filter runs at order {@link Ordered#HIGHEST_PRECEDENCE} + 10 — after
 * encoding/character-set filters but before all business filters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    /** Paths that do not require a JWT (health checks, metrics). */
    private static final Set<String> OPEN_PATHS = Set.of(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus"
    );

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final String expectedAudience;

    /**
     * @param jwkSource        JWKS source pointing at the Keycloak JWKS endpoint
     *                         (auto-configured by {@code SecurityConfig})
     * @param expectedAudience the OAuth2 audience value that tokens must contain
     */
    public TenantContextFilter(
            JWKSource<SecurityContext> jwkSource,
            @Value("${pharmapos.security.jwt.audience:pharmapos-api}") String expectedAudience
    ) {
        this.expectedAudience = expectedAudience;
        ConfigurableJWTProcessor<SecurityContext> proc = new DefaultJWTProcessor<>();
        proc.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        this.jwtProcessor = proc;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            // Checks 2 & 3: signature + expiry via Nimbus
            JWTClaimsSet claims = jwtProcessor.process(token, null);

            // Check 4: audience
            List<String> aud = claims.getAudience();
            if (aud == null || !aud.contains(expectedAudience)) {
                log.warn("JWT audience mismatch: expected={} got={}", expectedAudience, aud);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token audience");
                return;
            }

            // Check 5: tenant_id claim vs host subdomain
            String tokenTenantId = claims.getStringClaim("tenant_id");
            if (tokenTenantId == null || tokenTenantId.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Token missing tenant_id claim");
                return;
            }
            String hostTenant = extractTenantFromHost(request);
            if (hostTenant != null && !hostTenant.equalsIgnoreCase(tokenTenantId)) {
                log.warn("Tenant mismatch: host={} token={} src={}",
                    hostTenant, tokenTenantId, request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Tenant mismatch between token and host");
                return;
            }

            UUID tenantId = UUID.fromString(tokenTenantId);
            UUID userId   = UUID.fromString(claims.getSubject());
            UUID branchId = optUuid(claims, "branch_id");
            UUID deviceId = optUuid(claims, "device_id");

            @SuppressWarnings("unchecked")
            List<String> roleList = (List<String>) claims.getClaim("roles");
            Set<String> roles = roleList != null ? Set.copyOf(roleList) : Set.of();

            @SuppressWarnings("unchecked")
            List<String> grantList = (List<String>) claims.getClaim("branch_grants");
            Set<String> branchGrants = grantList != null ? Set.copyOf(grantList) : Set.of();

            TenantContextHolder.set(
                new TenantContext(tenantId, branchId, userId, roles, deviceId, branchGrants));

            try {
                chain.doFilter(request, response);
            } finally {
                // Always clear — pooled thread must not carry stale context (TDD §5.1 step 5)
                TenantContextHolder.clear();
            }

        } catch (BadJOSEException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } catch (ParseException e) {
            log.warn("JWT parse error: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Malformed token");
        } catch (JOSEException e) {
            log.error("JWT processing error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token processing error");
        }
    }

    /**
     * Extracts the first subdomain segment from the Host header.
     * {@code acme.pharmapos.com} → {@code acme}.
     * Returns {@code null} when the host is localhost or a bare IP (dev mode — check skipped).
     */
    private String extractTenantFromHost(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null) return null;
        int colon = host.indexOf(':');
        if (colon > 0) host = host.substring(0, colon);
        if ("localhost".equals(host) || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return null;
        }
        String[] parts = host.split("\\.");
        return parts.length >= 3 ? parts[0] : null;
    }

    private UUID optUuid(JWTClaimsSet claims, String claim) throws ParseException {
        return Optional.ofNullable(claims.getStringClaim(claim))
            .filter(s -> !s.isBlank())
            .map(UUID::fromString)
            .orElse(null);
    }
}
