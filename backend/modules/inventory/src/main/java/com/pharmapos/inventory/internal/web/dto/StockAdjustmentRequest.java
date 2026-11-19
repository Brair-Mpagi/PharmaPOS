package com.pharmapos.inventory.internal.web.dto;

import com.pharmapos.shared.valueobject.Quantity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for stock adjustment.
 */
@Data
public class StockAdjustmentRequest {

    @NotNull(message = "Adjustment quantity is required")
    private Quantity adjustmentQuantity;

    @NotBlank(message = "Reason code is required")
    private String reasonCode;

    private String note;
}
