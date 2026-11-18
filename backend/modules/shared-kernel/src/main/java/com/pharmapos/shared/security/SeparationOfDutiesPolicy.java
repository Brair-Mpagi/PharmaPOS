package com.pharmapos.shared.security;

import java.util.UUID;

/**
 * Separation-of-Duties policy interface (TDD §9.2).
 *
 * <p>Separation of Duties (SoD) rules prevent the same person from both
 * initiating and approving a sensitive action. These rules are coded as explicit
 * policy objects — not ad-hoc {@code if} statements — so they are testable in
 * isolation and appear in the generated permission-matrix test suite.
 *
 * <h3>Canonical rules (from Plan §3.4):</h3>
 * <ul>
 *   <li>The creator of a purchase order cannot approve it.</li>
 *   <li>The person who counted stock cannot approve their own variance.</li>
 *   <li>A user cannot approve their own refund.</li>
 *   <li>A user cannot approve their own FEFO override.</li>
 * </ul>
 *
 * <p>Implementations query the persistent record for the entity
 * (e.g. {@code prc_purchase_order.created_by}) and compare it to
 * {@code userId}. The check runs in the application layer's use-case service,
 * inside the same transaction as the approval action.
 *
 * <p>The default implementation, {@link DefaultSeparationOfDutiesPolicy}, is
 * autowired. Bounded-context modules may supply their own implementations
 * and qualify them appropriately.
 */
public interface SeparationOfDutiesPolicy {

    /**
     * Returns {@code true} if {@code userId} is allowed to approve the entity
     * identified by {@code entityId}, i.e. the user is not the same person who
     * created the entity.
     *
     * @param entityId UUID of the entity awaiting approval (e.g. PO id, refund id,
     *                 stock-count variance id)
     * @param userId   UUID of the user attempting to approve
     * @param entityType canonical type name used for the lookup query (e.g.
     *                 {@code "prc_purchase_order"}, {@code "inv_stock_count"},
     *                 {@code "sal_refund"})
     * @return {@code true} if approval is permitted; {@code false} if the user
     *         created the entity and therefore cannot approve it
     * @throws EntityNotFoundException if no entity with the given id/type is found
     */
    boolean canApprove(UUID entityId, UUID userId, String entityType);

    /**
     * Convenience check that throws {@link SodViolationException} instead of
     * returning {@code false}, matching the pattern used in use-case services.
     */
    default void assertCanApprove(UUID entityId, UUID userId, String entityType) {
        if (!canApprove(entityId, userId, entityType)) {
            throw new SodViolationException(
                "Separation-of-duties violation: user " + userId +
                " cannot approve their own " + entityType + " (" + entityId + ")");
        }
    }

    // ── Exception types ──────────────────────────────────────────────────────

    class SodViolationException extends RuntimeException {
        public SodViolationException(String msg) { super(msg); }
    }

    class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String msg) { super(msg); }
    }
}
