package com.pharmapos.inventory.internal.web;

import com.pharmapos.inventory.internal.application.InventoryService;
import com.pharmapos.inventory.internal.application.allocation.FEFOAllocationEngine;
import com.pharmapos.inventory.internal.domain.StockBalance;
import com.pharmapos.inventory.internal.domain.StockBatch;
import com.pharmapos.inventory.internal.domain.StockMovement;
import com.pharmapos.inventory.internal.web.dto.*;
import com.pharmapos.shared.valueobject.Quantity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Inventory operations.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    // =========================================================================
    // Stock Receipt
    // =========================================================================

    @PostMapping("/branches/{branchId}/stock/receive")
    @PreAuthorize("hasAuthority('stock.receive')")
    public ResponseEntity<StockBatchResponse> receiveStock(
            @PathVariable UUID branchId,
            @Valid @RequestBody StockReceiptRequest request) {

        log.debug("Receiving stock for branch {}, item {}", branchId, request.getSellableItemId());

        StockBatch batch = inventoryService.receiveStock(
                branchId,
                request.getSellableItemId(),
                request.getBatchNumber(),
                request.getExpiryDate(),
                request.getQuantity(),
                request.getUnitCost(),
                request.getSupplierId(),
                request.getGrnLineId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StockBatchResponse.from(batch));
    }

    // =========================================================================
    // FEFO Allocation
    // =========================================================================

    @PostMapping("/branches/{branchId}/stock/allocate")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<AllocationResponse> allocateStock(
            @PathVariable UUID branchId,
            @Valid @RequestBody StockAllocationRequest request) {

        log.debug("Allocating stock for branch {}, item {}, quantity {}",
                branchId, request.getSellableItemId(), request.getQuantity());

        FEFOAllocationEngine.AllocationResult result = inventoryService.allocateStock(
                branchId,
                request.getSellableItemId(),
                request.getQuantity()
        );

        return ResponseEntity.ok(AllocationResponse.from(result));
    }

    // =========================================================================
    // Stock Adjustment
    // =========================================================================

    @PostMapping("/branches/{branchId}/batches/{batchId}/adjust")
    @PreAuthorize("hasAuthority('stock.adjust')")
    public ResponseEntity<Void> adjustStock(
            @PathVariable UUID branchId,
            @PathVariable UUID batchId,
            @Valid @RequestBody StockAdjustmentRequest request) {

        log.debug("Adjusting stock for batch {}, adjustment {}", batchId, request.getAdjustmentQuantity());

        inventoryService.adjustStock(
                branchId,
                batchId,
                request.getAdjustmentQuantity(),
                request.getReasonCode(),
                request.getNote()
        );

        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // Quarantine Management
    // =========================================================================

    @PostMapping("/branches/{branchId}/batches/{batchId}/quarantine")
    @PreAuthorize("hasAuthority('stock.quarantine')")
    public ResponseEntity<Void> quarantineBatch(
            @PathVariable UUID branchId,
            @PathVariable UUID batchId,
            @Valid @RequestBody QuarantineRequest request) {

        log.debug("Quarantining batch {}, reason: {}", batchId, request.getReason());

        inventoryService.quarantineBatch(branchId, batchId, request.getReason());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/branches/{branchId}/batches/{batchId}/release")
    @PreAuthorize("hasAuthority('stock.quarantine')")
    public ResponseEntity<Void> releaseBatch(
            @PathVariable UUID branchId,
            @PathVariable UUID batchId) {

        log.debug("Releasing batch {} from quarantine", batchId);

        inventoryService.releaseBatch(branchId, batchId);

        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // Expiry Management
    // =========================================================================

    @PostMapping("/branches/{branchId}/stock/mark-expired")
    @PreAuthorize("hasAuthority('stock.write-off')")
    public ResponseEntity<Void> markExpiredBatches(@PathVariable UUID branchId) {

        log.debug("Marking expired batches for branch {}", branchId);

        inventoryService.markExpiredBatches(branchId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/branches/{branchId}/stock/expiring-soon")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<List<StockBatchResponse>> getExpiringSoonBatches(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "90") int daysThreshold) {

        List<StockBatch> batches = inventoryService.getExpiringSoonBatches(branchId, daysThreshold);
        List<StockBatchResponse> response = batches.stream()
                .map(StockBatchResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @GetMapping("/branches/{branchId}/items/{itemId}/batches")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<List<StockBatchResponse>> getBatchesForItem(
            @PathVariable UUID branchId,
            @PathVariable UUID itemId) {

        List<StockBatch> batches = inventoryService.getBatchesForItem(branchId, itemId);
        List<StockBatchResponse> response = batches.stream()
                .map(StockBatchResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/branches/{branchId}/batches/{batchId}")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<StockBatchResponse> getBatchById(
            @PathVariable UUID branchId,
            @PathVariable UUID batchId) {

        return inventoryService.getBatchById(branchId, batchId)
                .map(StockBatchResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/branches/{branchId}/batches/{batchId}/balance")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<StockBalanceResponse> getBatchBalance(
            @PathVariable UUID branchId,
            @PathVariable UUID batchId) {

        return inventoryService.getBatchBalance(branchId, batchId)
                .map(StockBalanceResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/branches/{branchId}/items/{itemId}/balances")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<List<StockBalanceResponse>> getBalancesForItem(
            @PathVariable UUID branchId,
            @PathVariable UUID itemId) {

        List<StockBalance> balances = inventoryService.getBalancesForItem(branchId, itemId);
        List<StockBalanceResponse> response = balances.stream()
                .map(StockBalanceResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/batches/{batchId}/history")
    @PreAuthorize("hasAuthority('stock.read')")
    public ResponseEntity<List<StockMovement>> getBatchHistory(@PathVariable UUID batchId) {

        List<StockMovement> history = inventoryService.getBatchHistory(batchId);

        return ResponseEntity.ok(history);
    }
}
