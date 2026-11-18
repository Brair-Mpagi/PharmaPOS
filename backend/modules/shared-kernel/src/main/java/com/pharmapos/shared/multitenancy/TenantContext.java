package com.pharmapos.shared.multitenancy;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, request-scoped tenant context built once from the validated JWT
 * and the host subdomain. Carried in ThreadLocal + Spring RequestScope (TDD §5.1).
 *
 * @param tenantId     UUID of the tenant extracted from the JWT {@code tenant_id} claim
 * @param branchId     UUID of the branch the caller is operating on (nullable for back-office)
 * @param userId       UUID of the authenticated user (JWT {@code sub})
 * @param roles        flattened set of permission strings the user holds across all branch assignments
 * @param deviceId     UUID of the enrolled device (nullable for browser sessions)
 * @param branchGrants set of branch UUID strings this user is explicitly granted access to;
 *                     an empty set means tenant-wide access (owner / admin)
 */
public record TenantContext(
        UUID tenantId,
        UUID branchId,
        UUID userId,
        Set<String> roles,
        UUID deviceId,
        Set<String> branchGrants
) {
    /** Canonical constructor — defensively copies sets and enforces non-null invariants. */
    public TenantContext {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId,   "userId must not be null");
        roles        = roles        == null ? Set.of()
                                           : Collections.unmodifiableSet(Set.copyOf(roles));
        branchGrants = branchGrants == null ? Set.of()
                                           : Collections.unmodifiableSet(Set.copyOf(branchGrants));
    }

    /**
     * Returns {@code true} if the caller's role set contains the given permission string.
     * The flattened permission set is resolved by the IAM module at context-build time.
     */
    public boolean hasPermission(String permission) {
        return roles.contains(permission);
    }

    /**
     * Returns {@code true} when the caller is allowed to operate on {@code targetBranchId}.
     * An empty {@code branchGrants} set means tenant-wide access (owner, admin).
     */
    public boolean hasBranchAccess(UUID targetBranchId) {
        if (targetBranchId == null) return false;
        return branchGrants.isEmpty() || branchGrants.contains(targetBranchId.toString());
    }
}
