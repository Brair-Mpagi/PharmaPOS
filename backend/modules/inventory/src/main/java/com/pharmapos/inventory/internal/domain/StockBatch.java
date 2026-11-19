package com.pharmapos.inventory.internal.domain;

import com.pharmapos.shared.domain.BranchEntity;
import com.pharmapos.shared.valueobject.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Stock Batch - Physical batch of stock with expiry tracking.
 * Each batch is uniquely identified by item, batch number, and supplier.
 */
@Entity
@Table(name = "inv_stock_batch")
@Getter
@Setter
public class StockBatch extends BranchEntity {

    @Column(name = "sellable_item_id", nullable = false)
    private UUID sellableItemId;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "grn_line_id")
    private UUID grnLineId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_cost_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money unitCost;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.AVAILABLE;

    @Column(name = "quarantine_reason")
    private String quarantineReason;

    public enum BatchStatus {
        AVAILABLE,    // Can be sold
        RESERVED,     // Held for a sale/transfer
        QUARANTINED,  // Under review
        EXPIRED,      // Past expiry date
        RECALLED,     // Supplier recall
        DAMAGED       // Cannot be sold
    }

    /**
     * Check if batch is expired based on current date.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Check if batch is expiring soon (within days threshold).
     */
    public boolean isExpiringSoon(int daysThreshold) {
        if (expiryDate == null) {
            return false;
        }
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return expiryDate.isBefore(threshold);
    }

    /**
     * Check if batch can be allocated for sale.
     */
    public boolean isAvailableForSale() {
        return status == BatchStatus.AVAILABLE && !isExpired();
    }

    /**
     * Quarantine this batch.
     */
    public void quarantine(String reason) {
        this.status = BatchStatus.QUARANTINED;
        this.quarantineReason = reason;
    }

    /**
     * Release batch from quarantine.
     */
    public void release() {
        if (status == BatchStatus.QUARANTINED) {
            this.status = BatchStatus.AVAILABLE;
            this.quarantineReason = null;
        }
    }

    /**
     * Mark batch as expired.
     */
    public void markExpired() {
        this.status = BatchStatus.EXPIRED;
    }
}
