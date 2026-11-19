package com.pharmapos.edge.security;

import com.pharmapos.shared.idempotency.IdempotencyFilter;
import com.pharmapos.shared.multitenancy.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the edge (BLN) profile.
 *
 * <p>The BLN runs in a branch-local environment with trusted LAN terminals.
 * It still validates JWTs for user sessions (issued by the cloud Keycloak, cached
 * locally) and uses the same {@link TenantContextFilter} as the cloud. The key
 * differences from the cloud profile:
 * <ul>
 *   <li>No external CORS — requests come only from the LAN terminal bundle served
 *       by the BLN itself</li>
 *   <li>Rate limiting is in-process (no Redis)</li>
 *   <li>HTTPS is provided by the BLN's local CA cert (ADR-013)</li>
 *   <li>Sessions are stateless (same as cloud)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class EdgeSecurityConfig {

    private final TenantContextFilter tenantContextFilter;
    private final IdempotencyFilter   idempotencyFilter;

    public EdgeSecurityConfig(
            TenantContextFilter tenantContextFilter,
            IdempotencyFilter   idempotencyFilter
    ) {
        this.tenantContextFilter = tenantContextFilter;
        this.idempotencyFilter   = idempotencyFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain edgeSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            // LAN-only: no CORS configuration needed (same-origin from BLN-served PWA)
            .cors(cors -> cors.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            // Filter order: TenantContext (after Spring Security auth) → Idempotency
            .addFilterAfter(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(idempotencyFilter,   TenantContextFilter.class)
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(ct -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31_536_000)
                    .includeSubDomains(true)
                )
            );

        return http.build();
    }
}
