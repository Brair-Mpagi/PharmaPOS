package com.pharmapos.inventory.internal.web.dto;

import com.pharmapos.inventory.internal.domain.StockBalance;
import com.pharmapos.shared.valueobject.Quantity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for stock balances.
 */
@Data
@Builder
public class StockBalanceResponse {

    private UUID id;
    private UUID batchId;
    private Quantity quantityOnHand;
    private Quantity quantityReserved;
    private Quantity availableQuantity;
    private Instant lastMovementAt;

    public static StockBalanceResponse from(StockBalance balance) {
        return StockBalanceResponse.builder()
                .id(balance.getId())
                .batchId(balance.getBatchId())
                .quantityOnHand(balance.getQuantityOnHand())
                .quantityReserved(balance.getQuantityReserved())
                .availableQuantity(balance.getAvailableQuantity())
                .lastMovementAt(balance.getLastMovementAt())
                .build();
    }
}
