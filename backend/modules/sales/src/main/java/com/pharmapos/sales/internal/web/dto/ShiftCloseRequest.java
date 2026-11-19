package com.pharmapos.sales.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request to close a shift with reconciliation.
 */
public record ShiftCloseRequest(
    @NotNull(message = "Actual cash amount is required")
    @PositiveOrZero(message = "Actual cash must be zero or positive")
    Long actualCashMinorUnits,

    String closureNotes
) {}
