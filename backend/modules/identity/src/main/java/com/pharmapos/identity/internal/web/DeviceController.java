package com.pharmapos.identity.internal.web;

import com.pharmapos.identity.internal.application.EnrollDeviceUseCase;
import com.pharmapos.identity.internal.application.RevokeDeviceUseCase;
import com.pharmapos.identity.internal.domain.DeviceRegistration;
import com.pharmapos.identity.internal.persistence.DeviceRepository;
import com.pharmapos.shared.multitenancy.TenantContextHolder;
import com.pharmapos.shared.security.RequiresPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for device enrolment and management (TDD §6.2 "Identity").
 *
 * <ul>
 *   <li>{@code POST   /api/v1/identity/devices}       — enrol a new device</li>
 *   <li>{@code DELETE /api/v1/identity/devices/{id}}  — revoke a device</li>
 *   <li>{@code GET    /api/v1/identity/devices}       — list devices for the branch/tenant</li>
 * </ul>
 *
 * <p>All endpoints require the caller to hold the {@code device.manage} permission.
 */
@RestController
@RequestMapping("/api/v1/identity/devices")
@RequiresPermission("device.manage")
public class DeviceController {

    private final EnrollDeviceUseCase  enrollUseCase;
    private final RevokeDeviceUseCase  revokeUseCase;
    private final DeviceRepository     deviceRepository;

    public DeviceController(
            EnrollDeviceUseCase  enrollUseCase,
            RevokeDeviceUseCase  revokeUseCase,
            DeviceRepository     deviceRepository
    ) {
        this.enrollUseCase  = enrollUseCase;
        this.revokeUseCase  = revokeUseCase;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Enrols a new Branch Local Node device.
     *
     * <p>Request body fields:
     * <ul>
     *   <li>{@code branchId}    — UUID of the branch this device belongs to</li>
     *   <li>{@code name}        — human-readable device name</li>
     *   <li>{@code publicKeyPem} — PEM-encoded RSA or EC public key</li>
     * </ul>
     */
    @PostMapping
    public ResponseEntity<EnrollResponse> enroll(@RequestBody EnrollRequest req) {
        var ctx = TenantContextHolder.get();
        var result = enrollUseCase.enroll(
            ctx.tenantId(),
            req.branchId(),
            req.name(),
            req.publicKeyPem(),
            ctx.userId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EnrollResponse.from(result));
    }

    /**
     * Revokes the device identified by {@code id}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        var ctx = TenantContextHolder.get();
        revokeUseCase.revoke(ctx.tenantId(), id, ctx.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists all devices for the caller's tenant (and optionally branch).
     */
    @GetMapping
    public List<DeviceResponse> list(
            @RequestParam(required = false) UUID branchId
    ) {
        var ctx = TenantContextHolder.get();
        List<DeviceRegistration> devices = branchId != null
            ? deviceRepository.findAllByTenantIdAndBranchId(ctx.tenantId(), branchId)
            : deviceRepository.findAllByTenantIdAndBranchId(ctx.tenantId(), ctx.branchId());
        return devices.stream().map(DeviceResponse::from).toList();
    }

    // ── Request / Response records ──────────────────────────────────────────

    public record EnrollRequest(UUID branchId, String name, String publicKeyPem) {}

    public record EnrollResponse(
            UUID   deviceId,
            String certificatePem,
            LeaseResponse lease
    ) {
        static EnrollResponse from(EnrollDeviceUseCase.EnrollmentResult r) {
            return new EnrollResponse(
                r.device().getId(),
                r.device().getCertificatePem(),
                LeaseResponse.from(r.initialLease())
            );
        }
    }

    public record LeaseResponse(
            java.time.Instant issuedAt,
            java.time.Instant expiresAt,
            String wrappedKeyBase64,
            String signatureBase64
    ) {
        static LeaseResponse from(com.pharmapos.identity.internal.domain.Lease l) {
            return new LeaseResponse(
                l.issuedAt(), l.expiresAt(),
                java.util.Base64.getEncoder().encodeToString(l.wrappedKey()),
                java.util.Base64.getEncoder().encodeToString(l.signature())
            );
        }
    }

    public record DeviceResponse(
            UUID   id,
            UUID   branchId,
            String name,
            String status,
            java.time.Instant enrolledAt,
            java.time.Instant leaseExpiresAt
    ) {
        static DeviceResponse from(DeviceRegistration d) {
            return new DeviceResponse(
                d.getId(), d.getBranchId(), d.getName(),
                d.getStatus().name(), d.getEnrolledAt(), d.getLeaseExpiresAt()
            );
        }
    }
}
