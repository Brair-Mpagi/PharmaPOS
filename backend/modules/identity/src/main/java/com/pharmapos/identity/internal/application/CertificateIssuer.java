package com.pharmapos.identity.internal.application;

import java.security.PublicKey;
import java.util.UUID;

/**
 * Issues X.509 certificates for enrolled devices.
 *
 * <p>Implementations call the platform CA (or a managed certificate authority
 * such as AWS ACM PCA) to sign the device's public key (TDD §8.2).
 */
public interface CertificateIssuer {

    /**
     * Issues a device certificate.
     *
     * @param tenantId  UUID of the owning tenant
     * @param branchId  UUID of the branch
     * @param publicKey the device's public key
     * @return PEM-encoded X.509 certificate
     */
    String issue(UUID tenantId, UUID branchId, PublicKey publicKey);
}
