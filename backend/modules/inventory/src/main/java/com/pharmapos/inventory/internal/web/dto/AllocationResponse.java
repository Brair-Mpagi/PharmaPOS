package com.pharmapos.inventory.internal.web.dto;

import com.pharmapos.inventory.internal.application.allocation.FEFOAllocationEngine;
import com.pharmapos.shared.valueobject.Quantity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for FEFO allocation results.
 */
@Data
@Builder
public class AllocationResponse {

    private boolean fullyAllocated;
    private List<BatchAllocationDto> allocations;
    private Quantity shortfall;
    private List<String> warnings;

    @Data
    @Builder
    public static class BatchAllocationDto {
        private UUID batchId;
        private Quantity quantity;
        private LocalDate expiryDate;
        private String batchNumber;
    }

    public static AllocationResponse from(FEFOAllocationEngine.AllocationResult result) {
        List<BatchAllocationDto> allocations = result.getAllocations().stream()
                .map(a -> BatchAllocationDto.builder()
                        .batchId(a.getBatchId())
                        .quantity(a.getQuantity())
                        .expiryDate(a.getExpiryDate())
                        .batchNumber(a.getBatchNumber())
                        .build())
                .toList();

        return AllocationResponse.builder()
                .fullyAllocated(result.isFullyAllocated())
                .allocations(allocations)
                .shortfall(result.getShortfall())
                .warnings(result.getWarnings())
                .build();
    }
}
