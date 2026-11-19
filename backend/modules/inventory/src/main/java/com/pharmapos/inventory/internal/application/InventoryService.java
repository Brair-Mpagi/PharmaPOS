package com.pharmapos.inventory.internal.application;

import com.pharmapos.inventory.internal.application.allocation.FEFOAllocationEngine;
import com.pharmapos.inventory.internal.domain.StockBalance;
import com.pharmapos.inventory.internal.domain.StockBatch;
import com.pharmapos.inventory.internal.domain.StockMovement;
import com.pharmapos.inventory.internal.persistence.StockBalanceRepository;
import com.pharmapos.inventory.internal.persistence.StockBatchRepository;
import com.pharmapos.inventory.internal.persistence.StockMovementRepository;
import com.pharmapos.shared.context.TenantContextHolder;
import com.pharmapos.shared.valueobject.Money;
import com.pharmapos.shared.valueobject.Quantity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inventory Service - Core stock management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryService {

    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final StockBalanceRepository balanceRepository;
    private final FEFOAllocationEngine fefoEngine;

    // =========================================================================
    // Stock Receipt
    // =========================================================================

    @Transactional
    public StockBatch receiveStock(
            UUID branchId,
            UUID sellableItemId,
            String batchNumber,
            LocalDate expiryDate,
            Quantity quantity,
            Money unitCost,
            UUID supplierId,
            UUID grnLineId) {

        UUID tenantId = TenantContextHolder.getContext().getTenantId();

        // Create or find batch
        StockBatch batch = new StockBatch();
        batch.setTenantId(tenantId);
        batch.setBranchId(branchId);
        batch.setSellableItemId(sellableItemId);
        batch.setBatchNumber(batchNumber);
        batch.setExpiryDate(expiryDate);
        batch.setUnitCost(unitCost);
        batch.setSupplierId(supplierId);
        batch.setGrnLineId(grnLineId);
        batch.setStatus(StockBatch.BatchStatus.AVAILABLE);

        batch = batchRepository.save(batch);

        // Record receipt movement
        recordMovement(
                branchId,
                batch.getId(),
                StockMovement.MovementType.RECEIPT,
                quantity,
                "GRN",
                grnLineId,
                null,
                "Stock received from supplier"
        );

        log.info("Received stock: batch={}, item={}, quantity={}", 
                batch.getId(), sellableItemId, quantity);

        return batch;
    }

    // =========================================================================
    // FEFO Allocation (for sales)
    // =========================================================================

    public FEFOAllocationEngine.AllocationResult allocateStock(
            UUID branchId,
            UUID sellableItemId,
            Quantity requestedQuantity) {

        UUID tenantId = TenantContextHolder.getContext().getTenantId();

        // Get available batches sorted by expiry (FEFO)
        List<StockBatch> availableBatches = batchRepository.findAvailableBatchesForFEFO(
                tenantId, branchId, sellableItemId, LocalDate.now()
        );

        if (availableBatches.isEmpty()) {
            log.warn("No available batches for item {} in branch {}", sellableItemId, branchId);
            FEFOAllocationEngine.AllocationResult result = new FEFOAllocationEngine.AllocationResult();
            result.setFullyAllocated(false);
            result.setAllocations(List.of());
            result.setShortfall(requestedQuantity);
            result.setWarnings(List.of("No stock available"));
            return result;
        }

        // Get balances for these batches
        List<UUID> batchIds = availableBatches.stream()
                .map(StockBatch::getId)
                .toList();

        List<StockBalance> balances = balanceRepository.findByTenantIdAndBranchIdAndBatchIdIn(
                tenantId, branchId, batchIds
        );

        // Run FEFO allocation
        FEFOAllocationEngine.AllocationResult result = fefoEngine.allocate(
                availableBatches,
                balances,
                requestedQuantity
        );

        log.debug("FEFO allocation for item {}: {} batches allocated, fully allocated: {}",
                sellableItemId, result.getAllocations().size(), result.isFullyAllocated());

        return result;
    }

    // =========================================================================
    // Stock Adjustment
    // =========================================================================

    @Transactional
    public void adjustStock(
            UUID branchId,
            UUID batchId,
            Quantity adjustmentQuantity,
            String reasonCode,
            String note) {

        UUID tenantId = TenantContextHolder.getContext().getTenantId();

        // Verify batch exists
        StockBatch batch = batchRepository.findByTenantIdAndBranchIdAndId(tenantId, branchId, batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        // Record adjustment movement
        recordMovement(
                branchId,
                batchId,
                StockMovement.MovementType.ADJUSTMENT,
                adjustmentQuantity,
                "ADJUSTMENT",
                null,
                reasonCode,
                note
        );

        log.info("Stock adjusted: batch={}, quantity={}, reason={}", 
                batchId, adjustmentQuantity, reasonCode);
    }

    // =========================================================================
    // Quarantine Management
    // =========================================================================

    @Transactional
    public void quarantineBatch(UUID branchId, UUID batchId, String reason) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();

        StockBatch batch = batchRepository.findByTenantIdAndBranchIdAndId(tenantId, branchId, batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        batch.quarantine(reason);
        batchRepository.save(batch);

        // Get current balance
        Optional<StockBalance> balanceOpt = balanceRepository
                .findByTenantIdAndBranchIdAndBatchId(tenantId, branchId, batchId);

        if (balanceOpt.isPresent() && balanceOpt.get().getQuantityOnHand().isPositive()) {
            // Record quarantine movement (negative)
            Quantity currentQty = balanceOpt.get().getQuantityOnHand();
            recordMovement(
                    branchId,
                    batchId,
                    StockMovement.MovementType.QUARANTINE,
                    currentQty.negate(),
                    "QUARANTINE",
                    null,
                    reason,
                    "Batch quarantined"
            );
        }

        log.info("Batch quarantined: batch={}, reason={}", batchId, reason);
    }

    @Transactional
    public void releaseBatch(UUID branchId, UUID batchId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();

        StockBatch batch = batchRepository.findByTenantIdAndBranchIdAndId(tenantId, branchId, batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() != StockBatch.BatchStatus.QUARANTINED) {
            throw new IllegalStateException("Batch is not quarantined");
        }

        batch.release();
        batchRepository.save(batch);

        log.info("Batch released from quarantine: batch={}", batchId);
    }

    // =========================================================================
    // Expiry Management
    // =========================================================================

    @Transactional
    public void markExpiredBatches(UUID branchId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();

        List<StockBatch> expiredBatches = batchRepository.findExpiredBatches(
                tenantId, branchId, LocalDate.now()
        );

        for (StockBatch batch : expiredBatches) {
            batch.markExpired();
            batchRepository.save(batch);

            // Write off expired stock
            Optional<StockBalance> balanceOpt = balanceRepository
                    .findByTenantIdAndBranchIdAndBatchId(tenantId, branchId, batch.getId());

            if (balanceOpt.isPresent() && balanceOpt.get().getQuantityOnHand().isPositive()) {
                Quantity qtyToWriteOff = balanceOpt.get().getQuantityOnHand();
                recordMovement(
                        branchId,
                        batch.getId(),
                        StockMovement.MovementType.EXPIRY_WRITE_OFF,
                        qtyToWriteOff.negate(),
                        "EXPIRY",
                        null,
                        "EXPIRED",
                        "Batch expired on " + batch.getExpiryDate()
                );
            }

            log.info("Batch marked expired and written off: batch={}, expiry={}",
                    batch.getId(), batch.getExpiryDate());
        }
    }

    public List<StockBatch> getExpiringSoonBatches(UUID branchId, int daysThreshold) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);

        return batchRepository.findExpiringSoon(
                tenantId, branchId, LocalDate.now(), threshold
        );
    }

    // =========================================================================
    // Queries
    // =========================================================================

    public List<StockBatch> getBatchesForItem(UUID branchId, UUID sellableItemId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return batchRepository.findByTenantIdAndBranchIdAndSellableItemId(
                tenantId, branchId, sellableItemId
        );
    }

    public Optional<StockBatch> getBatchById(UUID branchId, UUID batchId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return batchRepository.findByTenantIdAndBranchIdAndId(tenantId, branchId, batchId);
    }

    public Optional<StockBalance> getBatchBalance(UUID branchId, UUID batchId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return balanceRepository.findByTenantIdAndBranchIdAndBatchId(tenantId, branchId, batchId);
    }

    public List<StockBalance> getBalancesForItem(UUID branchId, UUID sellableItemId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return balanceRepository.findBalancesForItem(tenantId, branchId, sellableItemId);
    }

    public List<StockMovement> getBatchHistory(UUID batchId) {
        return movementRepository.findByBatchIdOrderByOccurredAtDesc(batchId);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    @Transactional
    public void recordMovement(
            UUID branchId,
            UUID batchId,
            StockMovement.MovementType type,
            Quantity quantity,
            String sourceDocType,
            UUID sourceDocId,
            String reasonCode,
            String note) {

        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        UUID userId = TenantContextHolder.getContext().getUserId();

        StockMovement movement = new StockMovement();
        movement.setTenantId(tenantId);
        movement.setBranchId(branchId);
        movement.setBatchId(batchId);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setSourceDocType(sourceDocType);
        movement.setSourceDocId(sourceDocId);
        movement.setReasonCode(reasonCode);
        movement.setNote(note);
        movement.setOccurredAt(Instant.now());
        movement.setRecordedAt(Instant.now());

        movementRepository.save(movement);

        log.debug("Stock movement recorded: type={}, batch={}, quantity={}",
                type, batchId, quantity);
    }
}
