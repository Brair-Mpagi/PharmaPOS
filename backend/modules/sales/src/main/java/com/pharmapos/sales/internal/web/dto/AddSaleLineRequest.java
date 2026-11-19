package com.pharmapos.sales.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * Request to add a line to a sale.
 */
public record AddSaleLineRequest(
    @NotNull(message = "Item ID is required")
    UUID itemId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity,

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be zero or positive")
    Long unitPriceMinorUnits
) {}
