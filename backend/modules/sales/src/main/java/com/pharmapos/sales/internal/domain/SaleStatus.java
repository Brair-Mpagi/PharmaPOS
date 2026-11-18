package com.pharmapos.sales.internal.domain;

/**
 * Lifecycle states for a sale document.
 * <p>
 * Transitions:
 * DRAFT → SUSPENDED (save basket for later)
 * DRAFT → COMPLETED (finalise)
 * DRAFT → VOIDED
 * SUSPENDED → DRAFT (resume)
 * SUSPENDED → VOIDED
 * COMPLETED is terminal — cannot be voided, only refunded.
 */
public enum SaleStatus {
    /** Basket being built on the terminal; not yet finalised. */
    DRAFT,
    /** Basket saved aside while another customer is served. */
    SUSPENDED,
    /** Successfully finalised; stock decremented, payment confirmed. */
    COMPLETED,
    /** Cancelled before completion; stock was never allocated. */
    VOIDED
}
