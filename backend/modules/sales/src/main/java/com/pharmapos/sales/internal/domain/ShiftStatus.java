package com.pharmapos.sales.internal.domain;

/**
 * Status of a cashier shift.
 */
public enum ShiftStatus {
    /** Shift is currently open and accepting sales. */
    OPEN,
    /** Shift has been closed; no further sales allowed. */
    CLOSED
}
