package com.pharmapos.identity.internal.application;

import com.pharmapos.identity.api.events.DeviceRevokedEvent;
import com.pharmapos.identity.internal.domain.DeviceRegistration;
import com.pharmapos.identity.internal.domain.Lease;
import com.pharmapos.identity.internal.persistence.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * Enrols a new Branch Local Node device (TDD §8.2).
 *
 * <h3>Steps:</h3>
 * <ol>
 *   <li>Validate the submitted public key (RSA or EC, PEM or DER).</li>
 *   <li>Issue an X.509 certificate signed by the platform CA.</li>
 *   <li>Create a 72-hour lease with the wrapped data-encryption key.</li>
 *   <li>Persist the {@link DeviceRegistration}.</li>
 * </ol>
 */
@Service
public class EnrollDeviceUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnrollDeviceUseCase.class);

    private final DeviceRepository      deviceRepository;
    private final CertificateIssuer     certificateIssuer;
    private final LeaseIssuer           leaseIssuer;

    public EnrollDeviceUseCase(
            DeviceRepository      deviceRepository,
            CertificateIssuer     certificateIssuer,
            LeaseIssuer           leaseIssuer
    ) {
        this.deviceRepository  = deviceRepository;
        this.certificateIssuer = certificateIssuer;
        this.leaseIssuer       = leaseIssuer;
    }

    /**
     * @param tenantId      UUID of the owning tenant (from TenantContext — never from the caller body)
     * @param branchId      UUID of the branch this device belongs to
     * @param deviceName    human-readable name (e.g. "Counter 1")
     * @param publicKeyPem  PEM-encoded RSA or EC public key
     * @param enrolledBy    UUID of the admin user performing the enrolment
     * @return the persisted {@link DeviceRegistration} together with its initial {@link Lease}
     */
    @Transactional
    public EnrollmentResult enroll(
            UUID   tenantId,
            UUID   branchId,
            String deviceName,
            String publicKeyPem,
            UUID   enrolledBy
    ) {
        log.info("Enrolling device '{}' for tenant={} branch={}", deviceName, tenantId, branchId);

        // 1. Validate the public key
        PublicKey publicKey = parseAndValidatePublicKey(publicKeyPem);

        // 2. Issue X.509 certificate
        String certificatePem = certificateIssuer.issue(tenantId, branchId, publicKey);

        // 3. Create the initial lease (72 h default)
        Instant leaseExpiry = Instant.now().plusSeconds(Lease.DEFAULT_DURATION_SECONDS);
        Lease initialLease = leaseIssuer.issue(
            /* deviceId provisional — set after persist */ null,
            tenantId, branchId, leaseExpiry,
            Set.of("sales", "inventory", "clinical", "sync")
        );

        // 4. Persist DeviceRegistration
        DeviceRegistration device = new DeviceRegistration(
            tenantId, branchId, deviceName, publicKeyPem, certificatePem, leaseExpiry);
        device = deviceRepository.save(device);

        // Re-issue lease with the now-known deviceId
        Lease signedLease = leaseIssuer.issue(
            device.getId(), tenantId, branchId, leaseExpiry,
            Set.of("sales", "inventory", "clinical", "sync")
        );

        log.info("Device enrolled: id={} name='{}' tenant={}", device.getId(), deviceName, tenantId);
        return new EnrollmentResult(device, signedLease);
    }

    private PublicKey parseAndValidatePublicKey(String publicKeyPem) {
        try {
            // Strip PEM headers and decode Base64
            String stripped = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(stripped);

            // Try RSA first, then EC
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            try {
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            } catch (Exception e) {
                return KeyFactory.getInstance("EC").generatePublic(spec);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid public key PEM: " + e.getMessage(), e);
        }
    }

    /**
     * Result of a successful enrolment.
     *
     * @param device     the persisted device record
     * @param initialLease the first lease issued for this device
     */
    public record EnrollmentResult(DeviceRegistration device, Lease initialLease) {}
}
