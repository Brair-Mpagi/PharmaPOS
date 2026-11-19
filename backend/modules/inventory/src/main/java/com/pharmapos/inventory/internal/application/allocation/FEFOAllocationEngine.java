package com.pharmapos.inventory.internal.application.allocation;

import com.pharmapos.inventory.internal.domain.StockBatch;
import com.pharmapos.inventory.internal.domain.StockBalance;
import com.pharmapos.shared.valueobject.Quantity;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FEFO Allocation Engine - First Expired, First Out.
 * 
 * Allocates stock from batches in order of expiry date to minimize waste.
 * Critical business logic for pharmacy operations.
 */
@Slf4j
public class FEFOAllocationEngine {

    private static final int SHORT_DATED_THRESHOLD_DAYS = 90;

    /**
     * Allocate quantity across batches using FEFO algorithm.
     *
     * @param availableBatches List of batches sorted by expiry date (earliest first)
     * @param balances Stock balances for each batch
     * @param requestedQuantity Quantity to allocate
     * @return Allocation result with batch allocations and warnings
     */
    public AllocationResult allocate(
            List<StockBatch> availableBatches,
            List<StockBalance> balances,
            Quantity requestedQuantity) {

        log.debug("FEFO allocation requested for {} units", requestedQuantity.getValue());

        AllocationResult result = new AllocationResult();
        result.setAllocations(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        BigDecimal remaining = requestedQuantity.getValue();
        String unit = requestedQuantity.getUnit();

        for (int i = 0; i < availableBatches.size() && remaining.compareTo(BigDecimal.ZERO) > 0; i++) {
            StockBatch batch = availableBatches.get(i);
            StockBalance balance = findBalance(balances, batch.getId());

            if (balance == null || balance.isEmpty()) {
                log.debug("Skipping batch {} - no balance", batch.getId());
                continue;
            }

            Quantity available = balance.getAvailableQuantity();
            if (available.isZero()) {
                log.debug("Skipping batch {} - no available quantity", batch.getId());
                continue;
            }

            // Calculate allocation from this batch
            BigDecimal toAllocate = available.getValue().min(remaining);
            
            BatchAllocation allocation = BatchAllocation.builder()
                    .batchId(batch.getId())
                    .quantity(Quantity.of(toAllocate.doubleValue(), unit))
                    .expiryDate(batch.getExpiryDate())
                    .batchNumber(batch.getBatchNumber())
                    .build();

            result.getAllocations().add(allocation);
            remaining = remaining.subtract(toAllocate);

            // Check for short-dated warning
            if (batch.isExpiringSoon(SHORT_DATED_THRESHOLD_DAYS)) {
                result.getWarnings().add(
                    String.format("Batch %s expires on %s (short-dated)", 
                        batch.getBatchNumber(), batch.getExpiryDate())
                );
            }

            log.debug("Allocated {} from batch {}, remaining: {}", 
                toAllocate, batch.getId(), remaining);
        }

        // Check if allocation is complete
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            result.setFullyAllocated(false);
            result.setShortfall(Quantity.of(remaining.doubleValue(), unit));
            result.getWarnings().add(
                String.format("Insufficient stock: short by %s %s", remaining, unit)
            );
            log.warn("FEFO allocation incomplete - shortfall: {} {}", remaining, unit);
        } else {
            result.setFullyAllocated(true);
            log.debug("FEFO allocation complete - {} batches allocated", 
                result.getAllocations().size());
        }

        return result;
    }

    /**
     * Validate allocation is possible before attempting.
     */
    public boolean canAllocate(
            List<StockBalance> balances,
            BigDecimal requestedQuantity) {
        
        BigDecimal totalAvailable = balances.stream()
                .map(StockBalance::getAvailableQuantity)
                .map(Quantity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalAvailable.compareTo(requestedQuantity) >= 0;
    }

    /**
     * Calculate total available quantity across all batches.
     */
    public Quantity getTotalAvailable(List<StockBalance> balances) {
        if (balances.isEmpty()) {
            return Quantity.zero("EA");
        }

        BigDecimal total = balances.stream()
                .map(StockBalance::getAvailableQuantity)
                .map(Quantity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String unit = balances.get(0).getAvailableQuantity().getUnit();
        return Quantity.of(total.doubleValue(), unit);
    }

    private StockBalance findBalance(List<StockBalance> balances, UUID batchId) {
        return balances.stream()
                .filter(b -> b.getBatchId().equals(batchId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Result of FEFO allocation.
     */
    @Data
    public static class AllocationResult {
        private boolean fullyAllocated;
        private List<BatchAllocation> allocations;
        private Quantity shortfall;
        private List<String> warnings;

        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        public boolean hasShortfall() {
            return shortfall != null && shortfall.isPositive();
        }
    }

    /**
     * Individual batch allocation.
     */
    @Data
    @Builder
    public static class BatchAllocation {
        private UUID batchId;
        private Quantity quantity;
        private LocalDate expiryDate;
        private String batchNumber;
    }
}
