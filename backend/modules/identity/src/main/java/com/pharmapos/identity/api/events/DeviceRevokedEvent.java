package com.pharmapos.identity.api.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a device is revoked (TDD §3.2 "Identity and access" publishes).
 *
 * <p>The event is written to the transactional outbox in the same transaction as the
 * revocation (TDD §3.4). The outbox relay delivers it to:
 * <ul>
 *   <li>The sync gateway pull endpoint — so that branches on their next pull receive
 *       the revocation and stop trusting the device</li>
 *   <li>The notifications module — to alert the tenant owner</li>
 * </ul>
 *
 * <p>The sync gateway additionally quarantines any event arriving from the device
 * <em>after</em> {@link #revokedAt()} (TDD §8.5).
 *
 * @param deviceId   UUID of the revoked device
 * @param tenantId   UUID of the owning tenant
 * @param branchId   UUID of the branch the device was assigned to
 * @param revokedBy  UUID of the user who performed the revocation
 * @param revokedAt  server timestamp of the revocation
 */
public record DeviceRevokedEvent(
        UUID    deviceId,
        UUID    tenantId,
        UUID    branchId,
        UUID    revokedBy,
        Instant revokedAt
) {
    public DeviceRevokedEvent(UUID deviceId, UUID tenantId, UUID branchId, UUID revokedBy) {
        this(deviceId, tenantId, branchId, revokedBy, Instant.now());
    }
}
