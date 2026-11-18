package com.pharmapos.sales.internal.domain;

/**
 * State machine for a payment line within a sale.
 * <p>
 * CASH/CREDIT → CONFIRMED immediately.
 * CARD/MOBILE_MONEY → PENDING until provider callback.
 * PENDING → CONFIRMED | FAILED.
 * CONFIRMED → REVERSED (refund or chargeback).
 */
public enum PaymentState {
    /** Awaiting provider confirmation (card, mobile money). */
    PENDING,
    /** Provider confirmed the payment. */
    CONFIRMED,
    /** Provider rejected or the payment timed out. */
    FAILED,
    /** Payment was reversed (refund). */
    REVERSED
}
