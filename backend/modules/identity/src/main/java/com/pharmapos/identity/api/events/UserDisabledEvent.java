package com.pharmapos.identity.api.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a user is disabled (TDD §3.2 "Identity and access" publishes).
 *
 * <p>The event is written to the transactional outbox in the same transaction as the
 * disable action (TDD §3.4 / §9.1 "Offboarding"). Consumers:
 * <ul>
 *   <li>Sync gateway — propagates to all branches so the user's cached credentials
 *       are invalidated on the next pull</li>
 *   <li>Notification module — alerts the tenant owner</li>
 *   <li>Audit module — records the disablement event in the tamper-evident log</li>
 * </ul>
 *
 * @param userId      UUID of the disabled user
 * @param tenantId    UUID of the owning tenant
 * @param disabledBy  UUID of the administrator who performed the action
 * @param reason      mandatory reason string for the audit trail
 * @param disabledAt  server timestamp of the disablement
 */
public record UserDisabledEvent(
        UUID    userId,
        UUID    tenantId,
        UUID    disabledBy,
        String  reason,
        Instant disabledAt
) {
    public UserDisabledEvent(UUID userId, UUID tenantId, UUID disabledBy, String reason) {
        this(userId, tenantId, disabledBy, reason, Instant.now());
    }
}
