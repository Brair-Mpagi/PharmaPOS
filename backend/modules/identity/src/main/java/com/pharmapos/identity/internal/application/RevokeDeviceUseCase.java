package com.pharmapos.identity.internal.application;

import com.pharmapos.identity.api.events.DeviceRevokedEvent;
import com.pharmapos.identity.internal.domain.DeviceRegistration;
import com.pharmapos.identity.internal.persistence.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Revokes an enrolled device (TDD §8.2).
 *
 * <p>Revocation:
 * <ol>
 *   <li>Sets the device status to REVOKED and invalidates the lease.</li>
 *   <li>Publishes a {@link DeviceRevokedEvent} in the same transaction
 *       (via the transactional outbox pattern — TDD §3.4).</li>
 * </ol>
 *
 * <p>The sync gateway will reject any further events from a revoked device.
 * Branches are notified on their next pull. Any event originating from the
 * device after the revocation timestamp is quarantined for manual review (TDD §8.5).
 */
@Service
public class RevokeDeviceUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevokeDeviceUseCase.class);

    private final DeviceRepository       deviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RevokeDeviceUseCase(
            DeviceRepository       deviceRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.deviceRepository = deviceRepository;
        this.eventPublisher   = eventPublisher;
    }

    /**
     * Revokes the device identified by {@code deviceId} within the given tenant.
     *
     * @param tenantId   UUID of the tenant (from TenantContext — never from the caller body)
     * @param deviceId   UUID of the device to revoke
     * @param revokedBy  UUID of the administrator performing the revocation
     * @throws DeviceNotFoundException if no active device with that id exists for the tenant
     */
    @Transactional
    public void revoke(UUID tenantId, UUID deviceId, UUID revokedBy) {
        DeviceRegistration device = deviceRepository.findByIdAndTenantId(deviceId, tenantId)
            .orElseThrow(() -> new DeviceNotFoundException(
                "Device " + deviceId + " not found for tenant " + tenantId));

        if (device.getStatus() == DeviceRegistration.DeviceStatus.REVOKED) {
            log.warn("Device {} is already revoked — idempotent noop", deviceId);
            return; // idempotent
        }

        device.revoke(revokedBy);
        deviceRepository.save(device);

        // Publish the domain event. The outbox relay picks it up and delivers it
        // to the sync gateway pull endpoint so branches learn of the revocation.
        eventPublisher.publishEvent(new DeviceRevokedEvent(
            deviceId, tenantId, device.getBranchId(), revokedBy));

        log.info("Device revoked: id={} tenant={} by={}", deviceId, tenantId, revokedBy);
    }

    public static class DeviceNotFoundException extends RuntimeException {
        public DeviceNotFoundException(String msg) { super(msg); }
    }
}
