package com.pharmapos.sales.internal.domain;

/**
 * Payment methods supported at the POS.
 * CASH and CREDIT settle immediately at time of sale.
 * CARD and MOBILE_MONEY require async provider confirmation.
 */
public enum PaymentMethod {
    /** Physical cash handed over. */
    CASH,
    /** Mobile money (MTN, Airtel, etc.); requires online confirmation. */
    MOBILE_MONEY,
    /** Debit or credit card; requires online confirmation. */
    CARD,
    /** Credit account — customer pays later; requires customer + credit limit. */
    CREDIT
}
