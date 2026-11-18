package com.pharmapos.identity.internal.application;

import com.pharmapos.identity.internal.domain.Role;
import com.pharmapos.identity.internal.domain.RoleAssignment;
import com.pharmapos.identity.internal.persistence.RoleAssignmentRepository;
import com.pharmapos.identity.internal.persistence.RoleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Answers "does user U have permission P on branch B?" by looking up the user's
 * {@link RoleAssignment}s and the permissions carried by each {@link Role} (TDD §9.2).
 *
 * <p>This service is the authoritative in-process permission check. The
 * {@link com.pharmapos.shared.security.PermissionEnforcementAspect} relies on the
 * flattened permission set carried in the JWT (pre-computed by Keycloak at login time).
 * This service is used for more granular checks during use-case execution, e.g. to
 * verify branch-scoped permissions or value limits.
 *
 * <p>Results are cached with a short TTL to avoid a DB hit on every permission check
 * in a request. The cache is evicted when a role or assignment changes.
 */
@Service
public class PermissionMatrixService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleRepository           roleRepository;

    public PermissionMatrixService(
            RoleAssignmentRepository roleAssignmentRepository,
            RoleRepository           roleRepository
    ) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleRepository           = roleRepository;
    }

    /**
     * Returns {@code true} if {@code userId} holds {@code permission} on {@code branchId}.
     *
     * <p>A user holds a permission on a branch if any of their role assignments
     * for that branch (or for null = tenant-wide) includes the permission string.
     *
     * @param tenantId   must match the current TenantContext (enforced by RLS)
     * @param userId     the user to check
     * @param permission the permission string (e.g. "sales.refund")
     * @param branchId   the branch to check; null checks tenant-wide assignments only
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permissionMatrix",
               key = "'perm:' + #tenantId + ':' + #userId + ':' + #permission + ':' + #branchId")
    public boolean hasPermission(UUID tenantId, UUID userId, String permission, UUID branchId) {
        List<RoleAssignment> assignments =
            roleAssignmentRepository.findByUserIdAndTenantId(userId, tenantId);

        for (RoleAssignment assignment : assignments) {
            // Assignment is applicable if it's tenant-wide (null branchId)
            // or matches the requested branch
            boolean branchMatch = assignment.getBranchId() == null
                || assignment.getBranchId().equals(branchId);
            if (!branchMatch) continue;

            Role role = roleRepository.findById(assignment.getRoleId()).orElse(null);
            if (role != null && role.getPermissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the full set of permissions the user holds on the given branch
     * (union across all applicable role assignments).
     */
    @Transactional(readOnly = true)
    public Set<String> effectivePermissions(UUID tenantId, UUID userId, UUID branchId) {
        List<RoleAssignment> assignments =
            roleAssignmentRepository.findByUserIdAndTenantId(userId, tenantId);

        return assignments.stream()
            .filter(a -> a.getBranchId() == null || a.getBranchId().equals(branchId))
            .map(a -> roleRepository.findById(a.getRoleId()).orElse(null))
            .filter(java.util.Objects::nonNull)
            .flatMap(role -> role.getPermissions().stream())
            .collect(Collectors.toUnmodifiableSet());
    }
}
