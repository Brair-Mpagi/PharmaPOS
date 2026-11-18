package com.pharmapos.identity.internal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Enrolled device (Branch Local Node). Maps to {@code iam_device}.
 *
 * <p>Each BLN is enrolled once per branch. Enrolment generates a device key
 * pair and certificate used for mutual TLS on the sync gateway. A signed
 * {@link Lease} is issued at enrolment and renewed periodically (TDD §8.2).
 *
 * <p>No hard deletes: a revoked device retains its record for audit purposes.
 * The status transitions are: ACTIVE → REVOKED (irreversible without
 * re-enrolment creating a new record).
 */
@Entity
@Table(
    name = "iam_device",
    indexes = {
        @Index(name = "idx_iam_dev_tenant_branch", columnList = "tenant_id, branch_id"),
        @Index(name = "idx_iam_dev_status",        columnList = "status")
    }
)
public class DeviceRegistration {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    /** Human-readable name assigned at enrolment (e.g. "Counter 1"). */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * PEM-encoded public key submitted by the device during enrolment.
     * Used to verify event signatures during sync.
     */
    @Column(name = "public_key_pem", nullable = false, columnDefinition = "text")
    private String publicKeyPem;

    /**
     * PEM-encoded X.509 certificate issued by the platform CA during enrolment.
     * Presented by the device for mutual TLS on the sync gateway (TDD §8.2).
     */
    @Column(name = "certificate_pem", nullable = false, columnDefinition = "text")
    private String certificatePem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    /** Set at REVOKED time. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** UUID of the user who revoked this device. */
    @Column(name = "revoked_by")
    private UUID revokedBy;

    /** Instant at which the current lease expires. Updated on each lease renewal. */
    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected DeviceRegistration() { /* JPA */ }

    public DeviceRegistration(
            UUID tenantId,
            UUID branchId,
            String name,
            String publicKeyPem,
            String certificatePem,
            Instant leaseExpiresAt
    ) {
        this.tenantId       = tenantId;
        this.branchId       = branchId;
        this.name           = name;
        this.publicKeyPem   = publicKeyPem;
        this.certificatePem = certificatePem;
        this.leaseExpiresAt = leaseExpiresAt;
        this.status         = DeviceStatus.ACTIVE;
        this.enrolledAt     = Instant.now();
    }

    // ── State transitions ────────────────────────────────────────────────────

    public void revoke(UUID revokedByUserId) {
        if (this.status == DeviceStatus.REVOKED) {
            throw new IllegalStateException("Device " + id + " is already revoked");
        }
        this.status      = DeviceStatus.REVOKED;
        this.revokedAt   = Instant.now();
        this.revokedBy   = revokedByUserId;
        this.leaseExpiresAt = Instant.now(); // immediately invalidate the lease
    }

    public void renewLease(Instant newExpiry) {
        if (this.status != DeviceStatus.ACTIVE) {
            throw new IllegalStateException("Cannot renew lease for device in status " + status);
        }
        this.leaseExpiresAt = newExpiry;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID         getId()             { return id; }
    public UUID         getTenantId()       { return tenantId; }
    public UUID         getBranchId()       { return branchId; }
    public String       getName()           { return name; }
    public String       getPublicKeyPem()   { return publicKeyPem; }
    public String       getCertificatePem() { return certificatePem; }
    public DeviceStatus getStatus()         { return status; }
    public Instant      getEnrolledAt()     { return enrolledAt; }
    public Instant      getRevokedAt()      { return revokedAt; }
    public UUID         getRevokedBy()      { return revokedBy; }
    public Instant      getLeaseExpiresAt() { return leaseExpiresAt; }
    public long         getVersion()        { return version; }

    public boolean isActive() { return status == DeviceStatus.ACTIVE; }

    public enum DeviceStatus { ACTIVE, REVOKED }
}
