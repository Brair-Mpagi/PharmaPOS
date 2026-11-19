package com.pharmapos.identity.internal.web;

import com.pharmapos.identity.internal.application.RenewLeaseUseCase;
import com.pharmapos.identity.internal.domain.Lease;
import com.pharmapos.shared.multitenancy.TenantContextHolder;
import com.pharmapos.shared.security.RequiresPermission;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

/**
 * REST controller for device lease renewal (TDD §8.4).
 *
 * <ul>
 *   <li>{@code POST /sync/v1/lease/renew} — renew the calling device's lease</li>
 * </ul>
 *
 * <p>This endpoint is called by the BLN's sync client before the lease expires.
 * The device must authenticate with its device certificate (mutual TLS); the
 * device certificate validates against the Keycloak JWKS or the platform CA.
 */
@RestController
@RequestMapping("/sync/v1/lease")
public class LeaseController {

    private final RenewLeaseUseCase renewLeaseUseCase;

    public LeaseController(RenewLeaseUseCase renewLeaseUseCase) {
        this.renewLeaseUseCase = renewLeaseUseCase;
    }

    /**
     * Renews the lease for the authenticated device.
     * The device ID is extracted from the TenantContext (populated by TenantContextFilter
     * from the JWT / device certificate claims).
     *
     * @return the new signed lease
     */
    @PostMapping("/renew")
    @RequiresPermission("device.sync")
    public LeaseRenewResponse renew() {
        var ctx      = TenantContextHolder.get();
        UUID deviceId = ctx.deviceId();
        if (deviceId == null) {
            throw new IllegalStateException("No device_id in token; cannot renew lease");
        }
        Lease lease = renewLeaseUseCase.renew(ctx.tenantId(), deviceId);
        return LeaseRenewResponse.from(lease);
    }

    // ── DTO ──────────────────────────────────────────────────────────────────

    public record LeaseRenewResponse(
            UUID   deviceId,
            UUID   tenantId,
            UUID   branchId,
            java.time.Instant issuedAt,
            java.time.Instant expiresAt,
            java.util.Set<String> allowedFeatures,
            String wrappedKeyBase64,
            String signatureBase64
    ) {
        static LeaseRenewResponse from(Lease l) {
            return new LeaseRenewResponse(
                l.deviceId(), l.tenantId(), l.branchId(),
                l.issuedAt(), l.expiresAt(), l.allowedFeatures(),
                Base64.getEncoder().encodeToString(l.wrappedKey()),
                Base64.getEncoder().encodeToString(l.signature())
            );
        }
    }
}
