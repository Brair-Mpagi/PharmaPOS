package com.pharmapos.cloud.config;

import com.pharmapos.shared.context.TenantContext;
import com.pharmapos.shared.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that extracts tenant context from JWT token and validates it against subdomain.
 * Sets up thread-local TenantContext for the request lifecycle.
 */
@Component
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated() 
                    && authentication.getPrincipal() instanceof Jwt jwt) {

                // Extract tenant ID from token
                String tenantIdStr = jwt.getClaimAsString("tenant_id");
                if (tenantIdStr == null) {
                    log.warn("JWT token missing tenant_id claim");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Missing tenant_id in token\"}");
                    return;
                }

                UUID tenantId = UUID.fromString(tenantIdStr);

                // Extract other claims
                String userIdStr = jwt.getSubject();
                UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
                String username = jwt.getClaimAsString("preferred_username");
                
                // Extract roles
                List<String> realmRoles = jwt.getClaimAsStringList("realm_roles");
                Set<String> roles = realmRoles != null ? new HashSet<>(realmRoles) : new HashSet<>();

                // Extract branch IDs (if present)
                List<String> branchIdsStr = jwt.getClaimAsStringList("branch_ids");
                Set<UUID> branchIds = new HashSet<>();
                if (branchIdsStr != null) {
                    branchIdsStr.forEach(id -> branchIds.add(UUID.fromString(id)));
                }

                // Extract device ID (if present - for BLN/terminal requests)
                String deviceIdStr = jwt.getClaimAsString("device_id");
                UUID deviceId = deviceIdStr != null ? UUID.fromString(deviceIdStr) : null;

                // Build tenant context
                TenantContext context = TenantContext.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .username(username)
                        .roles(roles)
                        .branchIds(branchIds)
                        .deviceId(deviceId)
                        .ipAddress(request.getRemoteAddr())
                        .build();

                TenantContextHolder.setContext(context);

                log.debug("Tenant context established: tenantId={}, userId={}, username={}", 
                        tenantId, userId, username);
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear context at end of request
            TenantContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for public endpoints
        return path.startsWith("/actuator/health") 
                || path.startsWith("/actuator/info")
                || path.equals("/health");
    }
}
