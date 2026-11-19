package com.pharmapos.shared.context;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

/**
 * Request-scoped tenant context.
 * Extracted from JWT token and validated against subdomain.
 * Immutable for the lifetime of the request.
 */
@Getter
@Builder
@ToString(exclude = "deviceId") // Exclude sensitive device ID from logs
public class TenantContext {

    private final UUID tenantId;
    private final UUID userId;
    private final String username;
    private final Set<String> roles;
    private final Set<UUID> branchIds;
    private final UUID deviceId;
    private final String ipAddress;

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles.
     */
    public boolean hasAnyRole(String... roles) {
        if (this.roles == null) return false;
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has access to a specific branch.
     */
    public boolean hasBranchAccess(UUID branchId) {
        return branchIds == null || branchIds.isEmpty() || branchIds.contains(branchId);
    }
}
