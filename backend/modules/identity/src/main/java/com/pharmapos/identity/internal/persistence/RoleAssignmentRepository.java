package com.pharmapos.identity.internal.persistence;

import com.pharmapos.identity.internal.domain.RoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link RoleAssignment} — tenant-scoped by RLS.
 */
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    List<RoleAssignment> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<RoleAssignment> findByUserIdAndBranchId(UUID userId, UUID branchId);

    void deleteByUserIdAndRoleIdAndBranchId(UUID userId, UUID roleId, UUID branchId);

    /**
     * Flags all pending approvals that are assigned to the given user, so a manager
     * can reassign them. Returns the number of records updated.
     *
     * <p>This method does a cross-module update. It uses a raw SQL query to avoid a
     * direct Spring Modulith dependency on other modules' entities. The query touches
     * only the pending_approval_queue which is owned by the shared-kernel audit module.
     */
    @Modifying
    @Query(nativeQuery = true, value =
        "UPDATE sys_pending_approval " +
        "SET assignee_id = NULL, flagged_for_reassignment = true, updated_at = now() " +
        "WHERE assignee_id = :userId AND tenant_id = :tenantId::uuid " +
        "  AND status = 'PENDING'")
    int flagPendingApprovalsForUser(
        @Param("userId")   UUID userId,
        @Param("tenantId") UUID tenantId
    );
}
