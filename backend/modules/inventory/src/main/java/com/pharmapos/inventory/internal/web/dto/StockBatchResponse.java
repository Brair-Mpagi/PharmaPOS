package com.pharmapos.inventory.internal.web.dto;

import com.pharmapos.inventory.internal.domain.StockBatch;
import com.pharmapos.shared.valueobject.Money;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for stock batches.
 */
@Data
@Builder
public class StockBatchResponse {

    private UUID id;
    private UUID tenantId;
    private UUID branchId;
    private UUID sellableItemId;
    private String batchNumber;
    private LocalDate expiryDate;
    private UUID supplierId;
    private Money unitCost;
    private String status;
    private String quarantineReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public static StockBatchResponse from(StockBatch batch) {
        return StockBatchResponse.builder()
                .id(batch.getId())
                .tenantId(batch.getTenantId())
                .branchId(batch.getBranchId())
                .sellableItemId(batch.getSellableItemId())
                .batchNumber(batch.getBatchNumber())
                .expiryDate(batch.getExpiryDate())
                .supplierId(batch.getSupplierId())
                .unitCost(batch.getUnitCost())
                .status(batch.getStatus().name())
                .quarantineReason(batch.getQuarantineReason())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .version(batch.getVersion())
                .build();
    }
}
