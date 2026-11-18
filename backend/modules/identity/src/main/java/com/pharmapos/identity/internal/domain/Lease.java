package com.pharmapos.identity.internal.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Signed lease granting a device the right to operate offline (TDD §8.2).
 *
 * <p>A lease is a value object — it is issued and replaced, never mutated.
 * It carries:
 * <ul>
 *   <li>the device's identity and scope (tenant, branch)</li>
 *   <li>the features the device is allowed to use while offline</li>
 *   <li>a wrapped data-encryption key (released to the device at renewal,
 *       kept in the OS key store; the local DB is unreadable after lease expiry)</li>
 *   <li>a platform signature over all of the above</li>
 * </ul>
 *
 * <p>Default lease duration: 72 hours. Maximum: 7 days (5 days for EFRIS/fiscal tenants).
 * High-risk roles are capped at 24 hours.
 *
 * <p>When a lease expires, the BLN enters <em>restricted mode</em>: lookup and
 * read-only views only, no new sales (TDD §8.2).
 */
public record Lease(
        UUID       deviceId,
        UUID       tenantId,
        UUID       branchId,
        Instant    issuedAt,
        Instant    expiresAt,
        Set<String> allowedFeatures,
        byte[]     wrappedKey,
        byte[]     signature
) {
    /** Default lease duration in seconds (72 hours). */
    public static final long DEFAULT_DURATION_SECONDS = 72L * 60 * 60;

    public Lease {
        Objects.requireNonNull(deviceId,  "deviceId");
        Objects.requireNonNull(tenantId,  "tenantId");
        Objects.requireNonNull(branchId,  "branchId");
        Objects.requireNonNull(issuedAt,  "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(signature, "signature");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        allowedFeatures = allowedFeatures == null
            ? Set.of()
            : Collections.unmodifiableSet(Set.copyOf(allowedFeatures));
        // Defensive copy of mutable byte arrays
        wrappedKey = wrappedKey != null ? wrappedKey.clone() : new byte[0];
        signature  = signature.clone();
    }

    /** Returns {@code true} if the lease has not yet expired. */
    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }

    /** Returns {@code true} if the lease is within the last 24 hours of its lifetime. */
    public boolean isNearExpiry() {
        return Instant.now().plusSeconds(24L * 60 * 60).isAfter(expiresAt);
    }

    /** Returns {@code true} if the device is allowed to use the named feature offline. */
    public boolean allows(String feature) {
        return allowedFeatures.contains(feature);
    }
}
