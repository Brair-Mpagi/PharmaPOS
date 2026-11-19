package com.pharmapos.inventory.internal.web.dto;

import com.pharmapos.shared.valueobject.Quantity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for FEFO stock allocation.
 */
@Data
public class StockAllocationRequest {

    @NotNull(message = "Sellable item ID is required")
    private UUID sellableItemId;

    @NotNull(message = "Quantity is required")
    private Quantity quantity;
}
