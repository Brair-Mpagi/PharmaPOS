package com.pharmapos.sales.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * Request to open a new shift.
 */
public record ShiftOpenRequest(
    @NotNull(message = "Branch ID is required")
    UUID branchId,

    @NotNull(message = "Cashier ID is required")
    UUID cashierId,

    @NotBlank(message = "Terminal ID is required")
    String terminalId,

    @NotNull(message = "Opening cash amount is required")
    @PositiveOrZero(message = "Opening cash must be zero or positive")
    Long openingCashMinorUnits
) {}
