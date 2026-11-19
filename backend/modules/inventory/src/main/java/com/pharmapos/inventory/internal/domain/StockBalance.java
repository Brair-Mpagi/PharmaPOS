package com.pharmapos.inventory.internal.domain;

import com.pharmapos.shared.domain.BranchEntity;
import com.pharmapos.shared.valueobject.Quantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Stock Balance - Current stock levels for a batch.
 * This is a projection/materialized view of stock movements.
 */
@Entity
@Table(name = "inv_stock_balance")
@Getter
@Setter
public class StockBalance extends BranchEntity {

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "quantity_on_hand_value", nullable = false)),
        @AttributeOverride(name = "unit", column = @Column(name = "quantity_on_hand_unit", nullable = false, length = 10))
    })
    private Quantity quantityOnHand;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "quantity_reserved_value", nullable = false)),
        @AttributeOverride(name = "unit", column = @Column(name = "quantity_reserved_unit", nullable = false, length = 10))
    })
    private Quantity quantityReserved;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    /**
     * Get available quantity (on hand minus reserved).
     */
    public Quantity getAvailableQuantity() {
        if (quantityOnHand == null) {
            return Quantity.zero(quantityReserved != null ? quantityReserved.getUnit() : "EA");
        }
        if (quantityReserved == null) {
            return quantityOnHand;
        }
        return quantityOnHand.subtract(quantityReserved);
    }

    /**
     * Check if sufficient quantity is available.
     */
    public boolean hasAvailable(BigDecimal quantity) {
        Quantity available = getAvailableQuantity();
        return available.getValue().compareTo(quantity) >= 0;
    }

    /**
     * Check if balance is zero.
     */
    public boolean isEmpty() {
        return quantityOnHand == null || quantityOnHand.isZero();
    }
}
