package com.pharmapos.inventory.internal.domain;

import com.pharmapos.shared.domain.BranchEntity;
import com.pharmapos.shared.valueobject.Quantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stock Movement - Immutable record of stock change.
 * All stock changes are recorded as movements, never modified.
 */
@Entity
@Table(name = "inv_stock_movement")
@Getter
@Setter
public class StockMovement extends BranchEntity {

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "movement_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "quantity_value", nullable = false)),
        @AttributeOverride(name = "unit", column = @Column(name = "quantity_unit", nullable = false, length = 10))
    })
    private Quantity quantity;

    @Column(name = "source_doc_type", length = 50)
    private String sourceDocType;

    @Column(name = "source_doc_id")
    private UUID sourceDocId;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Column(name = "origin_device_id")
    private UUID originDeviceId;

    @Column(name = "origin_seq")
    private Long originSeq;

    public enum MovementType {
        RECEIPT,           // Stock received from supplier
        SALE,              // Stock sold to customer
        RETURN,            // Customer return
        TRANSFER_OUT,      // Sent to another branch
        TRANSFER_IN,       // Received from another branch
        ADJUSTMENT,        // Manual adjustment
        DAMAGE,            // Damaged stock written off
        LOSS,              // Lost or stolen
        EXPIRY_WRITE_OFF,  // Expired stock disposed
        QUARANTINE,        // Moved to quarantine
        RELEASE            // Released from quarantine
    }

    /**
     * Check if this movement increases stock.
     */
    public boolean increasesStock() {
        return switch (movementType) {
            case RECEIPT, RETURN, TRANSFER_IN, RELEASE -> true;
            default -> false;
        };
    }

    /**
     * Check if this movement decreases stock.
     */
    public boolean decreasesStock() {
        return switch (movementType) {
            case SALE, TRANSFER_OUT, ADJUSTMENT, DAMAGE, LOSS, EXPIRY_WRITE_OFF, QUARANTINE -> true;
            default -> false;
        };
    }
}
