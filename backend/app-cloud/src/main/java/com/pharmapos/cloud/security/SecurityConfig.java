package com.pharmapos.cloud.security;

import com.pharmapos.shared.idempotency.IdempotencyFilter;
import com.pharmapos.shared.multitenancy.TenantContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the cloud profile (TDD §9.1, §9.2, §9.5).
 *
 * <h3>What this configures:</h3>
 * <ul>
 *   <li>JWT resource server against the Keycloak JWKS endpoint (stateless sessions)</li>
 *   <li>CORS — allows the terminal PWA and back-office origins</li>
 *   <li>Rate limiting via {@link RateLimitFilter} (per user, per device, per tenant, per IP)</li>
 *   <li>CSP nonce injection via {@link CspNonceFilter}</li>
 *   <li>Filter registration order: CspNonce → TenantContext → RateLimit → Idempotency → handlers</li>
 *   <li>All endpoints require authentication except open paths (/actuator/health, etc.)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TenantContextFilter tenantContextFilter;
    private final IdempotencyFilter   idempotencyFilter;
    private final RateLimitFilter     rateLimitFilter;
    private final CspNonceFilter      cspNonceFilter;

    @Value("${pharmapos.cors.terminal-origin:http://localhost:3000}")
    private String terminalOrigin;

    @Value("${pharmapos.cors.backoffice-origin:http://localhost:3001}")
    private String backofficeOrigin;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwksUri;

    public SecurityConfig(
            TenantContextFilter tenantContextFilter,
            IdempotencyFilter   idempotencyFilter,
            RateLimitFilter     rateLimitFilter,
            CspNonceFilter      cspNonceFilter
    ) {
        this.tenantContextFilter = tenantContextFilter;
        this.idempotencyFilter   = idempotencyFilter;
        this.rateLimitFilter     = rateLimitFilter;
        this.cspNonceFilter      = cspNonceFilter;
    }

    /**
     * Main API security filter chain (order 1 — evaluated first).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Session management ───────────────────────────────────────────
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── CSRF: disabled for stateless JWT API ─────────────────────────
            .csrf(csrf -> csrf.disable())

            // ── CORS ─────────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Authorization rules ──────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Open paths — no JWT required
                .requestMatchers(
                    "/actuator/health", "/actuator/info", "/actuator/prometheus"
                ).permitAll()
                // All other paths require a valid JWT
                .anyRequest().authenticated()
            )

            // ── JWT resource server ──────────────────────────────────────────
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )

            // ── Custom filters in order ──────────────────────────────────────
            // CspNonceFilter: earliest — sets CSP header before anything else
            .addFilterBefore(cspNonceFilter,      UsernamePasswordAuthenticationFilter.class)
            // TenantContextFilter: runs after Spring Security auth, sets TenantContext
            .addFilterAfter(tenantContextFilter,  UsernamePasswordAuthenticationFilter.class)
            // RateLimitFilter: runs after tenant is known
            .addFilterAfter(rateLimitFilter,      TenantContextFilter.class)
            // IdempotencyFilter: runs last of custom filters
            .addFilterAfter(idempotencyFilter,    RateLimitFilter.class)

            // ── Security headers ─────────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())         // X-Frame-Options: DENY
                .contentTypeOptions(ct -> {})                // X-Content-Type-Options: nosniff
                .httpStrictTransportSecurity(hsts -> hsts    // HSTS: 1 year + subdomains
                    .maxAgeInSeconds(31_536_000)
                    .includeSubDomains(true)
                    .preload(true)
                )
            );

        return http.build();
    }

    /**
     * CORS configuration: allow terminal PWA and back-office origins with the
     * methods, headers, and credentials needed for the API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(terminalOrigin, backofficeOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Idempotency-Key",
            "X-Request-Id", "traceparent"
        ));
        config.setExposedHeaders(List.of("X-Idempotent-Replay", "ETag", "traceparent"));
        config.setAllowCredentials(false); // JWT in Authorization header, not cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",  config);
        source.registerCorsConfiguration("/sync/**", config);
        return source;
    }

    /**
     * Converts JWT {@code roles} claim to Spring Security authorities.
     * The claim is a flat list of permission strings (pre-computed by Keycloak
     * via the IAM module's role-to-permission mapping).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix(""); // permissions are plain strings, no ROLE_ prefix

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
