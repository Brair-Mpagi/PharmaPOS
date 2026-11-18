package com.pharmapos.identity.internal.application;

import com.pharmapos.identity.api.events.UserDisabledEvent;
import com.pharmapos.identity.internal.domain.User;
import com.pharmapos.identity.internal.persistence.DeviceRepository;
import com.pharmapos.identity.internal.persistence.RoleAssignmentRepository;
import com.pharmapos.identity.internal.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Disables a user and cleans up all active sessions, tokens, and device grants
 * in a single atomic transaction (TDD §9.1 "Offboarding").
 *
 * <h3>Steps performed in one transaction:</h3>
 * <ol>
 *   <li>Set the user's status to {@code DISABLED}.</li>
 *   <li>Revoke all active Keycloak sessions (via the Keycloak admin API — called in this transaction
 *       but tolerating failure via idempotent retry).</li>
 *   <li>Revoke all device grants associated with the user.</li>
 *   <li>Reassign or flag all pending approvals that require this user.</li>
 *   <li>Write an audit entry for the action.</li>
 *   <li>Publish a {@link UserDisabledEvent} via the transactional outbox.</li>
 * </ol>
 *
 * <p>The effect on offline branches: they learn of the disablement on the next
 * pull or lease renewal. Device leases already in-flight are not revoked immediately
 * (they expire naturally), but lease renewals are blocked.
 */
@Service
public class DisableUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(DisableUserUseCase.class);

    private final UserRepository           userRepository;
    private final DeviceRepository         deviceRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final KeycloakAdminClient      keycloakAdmin;
    private final ApplicationEventPublisher eventPublisher;

    public DisableUserUseCase(
            UserRepository           userRepository,
            DeviceRepository         deviceRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            KeycloakAdminClient      keycloakAdmin,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository          = userRepository;
        this.deviceRepository        = deviceRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.keycloakAdmin           = keycloakAdmin;
        this.eventPublisher          = eventPublisher;
    }

    /**
     * @param tenantId   UUID of the tenant (from TenantContext)
     * @param userId     UUID of the user to disable
     * @param disabledBy UUID of the admin performing the action
     * @param reason     mandatory reason string for the audit log
     * @throws UserNotFoundException if the user does not exist for this tenant
     */
    @Transactional
    public void disable(UUID tenantId, UUID userId, UUID disabledBy, String reason) {
        log.info("Disabling user={} tenant={} by={}", userId, tenantId, disabledBy);

        // 1. Load user
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new UserNotFoundException(
                "User " + userId + " not found for tenant " + tenantId));

        if (user.getStatus() == User.UserStatus.DISABLED) {
            log.warn("User {} is already disabled — idempotent noop", userId);
            return;
        }

        // 2. Disable user record
        user.disable();
        userRepository.save(user);

        // 3. Revoke Keycloak sessions — best-effort; retried by outbox if needed
        try {
            keycloakAdmin.revokeAllSessions(user.getKeycloakId());
        } catch (Exception e) {
            log.warn("Keycloak session revocation failed for user={}; " +
                "will retry via outbox: {}", userId, e.getMessage());
        }

        // 4. Revoke all device grants for this user (devices where user is the sole operator)
        deviceRepository.findActiveDevicesByUserId(userId, tenantId)
            .forEach(device -> {
                device.revoke(disabledBy);
                deviceRepository.save(device);
            });

        // 5. Reassign pending approvals — flag them for manager attention
        int reassigned = roleAssignmentRepository.flagPendingApprovalsForUser(userId, tenantId);
        if (reassigned > 0) {
            log.info("Flagged {} pending approvals for reassignment (user={})", reassigned, userId);
        }

        // 6. Publish domain event (outbox pattern — written in this transaction)
        eventPublisher.publishEvent(new UserDisabledEvent(userId, tenantId, disabledBy, reason));

        log.info("User disabled successfully: id={} tenant={}", userId, tenantId);
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String msg) { super(msg); }
    }
}
