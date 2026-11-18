package com.pharmapos.identity.internal.application;

import com.pharmapos.identity.internal.domain.DeviceRegistration;
import com.pharmapos.identity.internal.domain.Lease;
import com.pharmapos.identity.internal.domain.TenantStatus;
import com.pharmapos.identity.internal.persistence.DeviceRepository;
import com.pharmapos.shared.multitenancy.TenantDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Renews a device lease (TDD §8.2, §8.4 endpoint {@code POST /sync/v1/lease/renew}).
 *
 * <h3>Pre-conditions checked before issuing a fresh lease:</h3>
 * <ol>
 *   <li>Device exists and is {@code ACTIVE}.</li>
 *   <li>Tenant is {@code ACTIVE} (locked tenants do not get renewed leases — offline
 *       operations are suspended until the account is restored).</li>
 *   <li>No compliance holds or outstanding critical alerts that block renewal.</li>
 * </ol>
 *
 * <p>The renewal issues a new {@link Lease} for the default 72-hour window (or the
 * tenant-configured window, subject to the ceilings in TDD §8.2). The
 * {@link DeviceRegistration#getLeaseExpiresAt()} is updated in the same transaction.
 */
@Service
public class RenewLeaseUseCase {

    private static final Logger log = LoggerFactory.getLogger(RenewLeaseUseCase.class);

    private final DeviceRepository  deviceRepository;
    private final TenantDirectory   tenantDirectory;
    private final LeaseIssuer       leaseIssuer;
    private final EntitlementService entitlementService;

    public RenewLeaseUseCase(
            DeviceRepository   deviceRepository,
            TenantDirectory    tenantDirectory,
            LeaseIssuer        leaseIssuer,
            EntitlementService entitlementService
    ) {
        this.deviceRepository   = deviceRepository;
        this.tenantDirectory    = tenantDirectory;
        this.leaseIssuer        = leaseIssuer;
        this.entitlementService = entitlementService;
    }

    /**
     * @param tenantId UUID of the tenant making the request
     * @param deviceId UUID of the device requesting renewal
     * @return a fresh {@link Lease} valid for the configured duration
     */
    @Transactional
    public Lease renew(UUID tenantId, UUID deviceId) {
        log.debug("Lease renewal requested: device={} tenant={}", deviceId, tenantId);

        // 1. Device must be ACTIVE
        DeviceRegistration device = deviceRepository.findByIdAndTenantId(deviceId, tenantId)
            .orElseThrow(() -> new RenewalException("Device " + deviceId + " not found"));

        if (!device.isActive()) {
            throw new RenewalException("Device " + deviceId + " is not active (status=" +
                device.getStatus() + "); lease renewal denied");
        }

        // 2. Tenant must be ACTIVE
        TenantDirectory.DataSourceConfig tenantCfg = tenantDirectory.resolve(tenantId);
        if (tenantCfg == null || tenantCfg.status() != TenantDirectory.TenantStatus.ACTIVE) {
            throw new RenewalException("Tenant " + tenantId + " is not active; lease renewal denied");
        }

        // 3. Entitlement / compliance check
        entitlementService.assertCanRenewLease(tenantId, deviceId);

        // 4. Issue fresh lease
        long leaseDurationSeconds = entitlementService.leaseDurationSeconds(tenantId);
        Instant expiry = Instant.now().plusSeconds(leaseDurationSeconds);

        Set<String> features = entitlementService.allowedOfflineFeatures(tenantId);
        Lease freshLease = leaseIssuer.issue(deviceId, tenantId, device.getBranchId(), expiry, features);

        // 5. Update the device record
        device.renewLease(expiry);
        deviceRepository.save(device);

        log.info("Lease renewed: device={} tenant={} expires={}", deviceId, tenantId, expiry);
        return freshLease;
    }

    public static class RenewalException extends RuntimeException {
        public RenewalException(String msg) { super(msg); }
    }
}
