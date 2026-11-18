package com.pharmapos.identity.internal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * IAM role. Maps to {@code iam_role}.
 *
 * <p>A role is a named, tenant-scoped set of permission strings. Role assignments
 * ({@link RoleAssignment}) associate a user with a role within a specific branch.
 * The permission model is: roles carry permissions; a role assignment is per branch
 * (TDD §9.2).
 */
@Entity
@Table(
    name = "iam_role",
    indexes = {
        @Index(name = "idx_iam_role_tenant_name", columnList = "tenant_id, name", unique = true)
    }
)
public class Role {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Human-readable, tenant-unique role name (e.g. "Cashier", "Pharmacist", "Branch Manager"). */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Set of permission strings carried by this role.
     * Stored as a PostgreSQL text array.
     * Each string must match a permission defined in the authoritative permission matrix (Plan §3.2).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "iam_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        indexes = @Index(name = "idx_iam_rp_role", columnList = "role_id")
    )
    @Column(name = "permission", nullable = false, length = 100)
    private Set<String> permissions = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected Role() { /* JPA */ }

    public Role(UUID tenantId, String name, Set<String> permissions) {
        this.tenantId    = tenantId;
        this.name        = name;
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }

    public void addPermission(String permission)    { this.permissions.add(permission); }
    public void removePermission(String permission) { this.permissions.remove(permission); }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID        getId()          { return id; }
    public UUID        getTenantId()    { return tenantId; }
    public String      getName()        { return name; }
    public Set<String> getPermissions() { return Collections.unmodifiableSet(permissions); }
    public long        getVersion()     { return version; }
}
