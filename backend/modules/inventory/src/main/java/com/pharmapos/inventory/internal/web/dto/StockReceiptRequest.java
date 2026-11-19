package com.pharmapos.inventory.internal.web.dto;

import com.pharmapos.shared.valueobject.Money;
import com.pharmapos.shared.valueobject.Quantity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for receiving stock.
 */
@Data
public class StockReceiptRequest {

    @NotNull(message = "Sellable item ID is required")
    private UUID sellableItemId;

    private String batchNumber;

    private LocalDate expiryDate;

    @NotNull(message = "Quantity is required")
    private Quantity quantity;

    @NotNull(message = "Unit cost is required")
    private Money unitCost;

    private UUID supplierId;

    private UUID grnLineId;
}
