package com.pharmapos.identity.internal.application;

import com.pharmapos.identity.internal.domain.Lease;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Issues and signs device leases.
 *
 * <p>Implementations use the platform KMS to sign the lease and wrap the
 * per-tenant data-encryption key (TDD §8.2).
 */
public interface LeaseIssuer {

    /**
     * Issues a signed lease for the given device.
     *
     * @param deviceId        UUID of the device (may be null during enrolment before the device is persisted)
     * @param tenantId        UUID of the tenant
     * @param branchId        UUID of the branch
     * @param expiresAt       instant at which the lease expires
     * @param allowedFeatures set of feature keys the device may use offline
     * @return a signed {@link Lease} ready to be delivered to the device
     */
    Lease issue(
        UUID       deviceId,
        UUID       tenantId,
        UUID       branchId,
        Instant    expiresAt,
        Set<String> allowedFeatures
    );
}
