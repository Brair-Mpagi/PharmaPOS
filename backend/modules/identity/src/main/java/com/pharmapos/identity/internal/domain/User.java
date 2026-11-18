package com.pharmapos.identity.internal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * IAM user record. Maps to {@code iam_user}.
 *
 * <p>The application does not store passwords — authentication is fully delegated
 * to Keycloak (TDD §9.1). This entity tracks the user's local status, MFA
 * enrolment, and their Keycloak subject ID so the application can correlate
 * tokens to internal users.
 */
@Entity
@Table(
    name = "iam_user",
    indexes = {
        @Index(name = "idx_iam_user_tenant_keycloak", columnList = "tenant_id, keycloak_id", unique = true),
        @Index(name = "idx_iam_user_tenant_email",    columnList = "tenant_id, email")
    }
)
public class User {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** The Keycloak subject identifier ({@code sub} claim from the JWT). */
    @Column(name = "keycloak_id", nullable = false, updatable = false, length = 255)
    private String keycloakId;

    @Column(nullable = false, length = 150)
    private String username;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected User() { /* JPA */ }

    public User(UUID tenantId, String keycloakId, String username, String email) {
        this.tenantId   = tenantId;
        this.keycloakId = keycloakId;
        this.username   = username;
        this.email      = email;
        this.status     = UserStatus.ACTIVE;
        this.mfaEnabled = false;
        this.createdAt  = Instant.now();
        this.updatedAt  = this.createdAt;
    }

    // ── State transitions ────────────────────────────────────────────────────

    public void disable() {
        this.status    = UserStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void lock() {
        this.status    = UserStatus.LOCKED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status    = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void enableMfa()  { this.mfaEnabled = true;  this.updatedAt = Instant.now(); }
    public void disableMfa() { this.mfaEnabled = false; this.updatedAt = Instant.now(); }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID       getId()         { return id; }
    public UUID       getTenantId()   { return tenantId; }
    public String     getKeycloakId() { return keycloakId; }
    public String     getUsername()   { return username; }
    public String     getEmail()      { return email; }
    public UserStatus getStatus()     { return status; }
    public boolean    isMfaEnabled()  { return mfaEnabled; }
    public Instant    getCreatedAt()  { return createdAt; }
    public Instant    getUpdatedAt()  { return updatedAt; }
    public long       getVersion()    { return version; }

    // ── Status enum ──────────────────────────────────────────────────────────

    public enum UserStatus { ACTIVE, DISABLED, LOCKED }
}
