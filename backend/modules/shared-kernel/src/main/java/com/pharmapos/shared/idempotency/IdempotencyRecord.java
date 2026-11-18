package com.pharmapos.shared.idempotency;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of an idempotency key seen by the server.
 *
 * <p>When a mutating request arrives with an {@code Idempotency-Key} header the
 * {@link IdempotencyFilter} checks this table first:
 * <ul>
 *   <li>Key seen + same fingerprint → return the stored response without re-executing</li>
 *   <li>Key seen + different fingerprint → 422 Unprocessable Entity</li>
 *   <li>Key not seen → record a new row and proceed (TDD §6.1 "Idempotency")</li>
 * </ul>
 *
 * <p>Rows are retained for at least 30 days (Plan NFR D3), then eligible for deletion
 * by a scheduled cleanup job.
 *
 * <p>Table: {@code sys_idempotency} — module prefix {@code sys_} to signal shared
 * infrastructure owned by the kernel, not any bounded context.
 */
@Entity
@Table(
    name = "sys_idempotency",
    indexes = {
        @Index(name = "idx_idmp_key_tenant", columnList = "idempotency_key, tenant_id", unique = true)
    }
)
public class IdempotencyRecord {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * The client-supplied idempotency key (UUIDv7 by convention).
     * Unique per tenant (a key is only idempotent within the tenant that created it).
     */
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * SHA-256 of {@code method + path + body} — used to detect a replay with
     * a different request body, which must be rejected with 422.
     */
    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    /** HTTP status code of the original response. */
    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    /** JSON body of the original response (stored for replay). */
    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    /** Content-Type of the original response (e.g. {@code application/json}). */
    @Column(name = "response_content_type", length = 128)
    private String responseContentType;

    /** Server time when this record was first created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Records become eligible for deletion after this instant (created_at + 30 days). */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() { /* JPA */ }

    public IdempotencyRecord(
            String idempotencyKey,
            UUID   tenantId,
            String requestFingerprint,
            int    responseStatus,
            String responseBody,
            String responseContentType
    ) {
        this.idempotencyKey     = idempotencyKey;
        this.tenantId           = tenantId;
        this.requestFingerprint = requestFingerprint;
        this.responseStatus     = responseStatus;
        this.responseBody       = responseBody;
        this.responseContentType = responseContentType;
        this.createdAt          = Instant.now();
        this.expiresAt          = this.createdAt.plusSeconds(30L * 24 * 60 * 60); // 30 days
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID   getId()                  { return id; }
    public String getIdempotencyKey()      { return idempotencyKey; }
    public UUID   getTenantId()            { return tenantId; }
    public String getRequestFingerprint()  { return requestFingerprint; }
    public int    getResponseStatus()      { return responseStatus; }
    public String getResponseBody()        { return responseBody; }
    public String getResponseContentType() { return responseContentType; }
    public Instant getCreatedAt()          { return createdAt; }
    public Instant getExpiresAt()          { return expiresAt; }
}
