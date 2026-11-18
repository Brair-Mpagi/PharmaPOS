package com.pharmapos.identity.internal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-branch role assignment. Maps to {@code iam_role_assignment}.
 *
 * <p>A role assignment grants a user a role within a specific branch.
 * This is the join between users, roles, and branches (TDD §9.2):
 * "a role assignment is per branch."
 *
 * <p>When a user is an owner or admin with tenant-wide access, the
 * {@code branchId} may be {@code null} to indicate unrestricted branch access.
 */
@Entity
@Table(
    name = "iam_role_assignment",
    indexes = {
        @Index(name = "idx_iam_ra_user",   columnList = "user_id"),
        @Index(name = "idx_iam_ra_role",   columnList = "role_id"),
        @Index(name = "idx_iam_ra_branch", columnList = "branch_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_iam_ra_user_role_branch",
            columnNames = {"user_id", "role_id", "branch_id"}
        )
    }
)
public class RoleAssignment {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    /**
     * The branch this assignment is scoped to, or {@code null} for tenant-wide.
     * NULL means the user holds this role across all branches.
     */
    @Column(name = "branch_id", updatable = false)
    private UUID branchId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    /** UUID of the user who created this assignment — audit trail. */
    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

    protected RoleAssignment() { /* JPA */ }

    public RoleAssignment(UUID userId, UUID roleId, UUID branchId, UUID assignedBy) {
        this.userId     = userId;
        this.roleId     = roleId;
        this.branchId   = branchId;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID    getId()         { return id; }
    public UUID    getUserId()     { return userId; }
    public UUID    getRoleId()     { return roleId; }
    public UUID    getBranchId()   { return branchId; }
    public Instant getAssignedAt() { return assignedAt; }
    public UUID    getAssignedBy() { return assignedBy; }
}
