package com.pharmapos.sales.internal.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to create a new sale draft.
 */
public record CreateSaleRequest(
    @NotNull(message = "Branch ID is required")
    UUID branchId,

    @NotNull(message = "Shift ID is required")
    UUID shiftId,

    UUID customerId  // Optional for walk-in customers
) {}
