package com.pharmapos.sharedkernel;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, request-scoped tenant context.
 *
 * <p>Built once per request from the validated bearer token and tenant sub-domain.
 * Never mutated. Contains all the scoping information needed for authorisation
 * checks and RLS parameter setting.
 *
 * @param tenantId   the tenant UUID (extracted from the token, verified against the sub-domain)
 * @param branchId   the branch UUID for branch-scoped operations (nullable for cloud-wide calls)
 * @param userId     the authenticated user UUID
 * @param roles      the user's roles for the current branch
 * @param deviceId   the device UUID when the request originates from a terminal or BLN (nullable)
 */
public record TenantContext(
        UUID tenantId,
        UUID branchId,
        UUID userId,
        Set<String> roles,
        UUID deviceId
) {
    public TenantContext {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        roles = roles == null ? Set.of() : Collections.unmodifiableSet(roles);
    }

    /**
     * Returns true if the user holds the given role in the current context.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Returns true if the context has a branch scope.
     */
    public boolean isBranchScoped() {
        return branchId != null;
    }

    /**
     * Returns true if the request originated from a device (terminal or BLN).
     */
    public boolean isDeviceRequest() {
        return deviceId != null;
    }
}
