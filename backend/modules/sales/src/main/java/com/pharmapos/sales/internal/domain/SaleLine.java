package com.pharmapos.sales.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import com.pharmapos.shared.valueobject.Money;
import com.pharmapos.shared.valueobject.Quantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sale Line - Individual item in a sale with batch allocation.
 */
@Entity
@Table(name = "sal_sale_line")
@Getter
@Setter
public class SaleLine extends TenantEntity {

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "sellable_item_id", nullable = false)
    private UUID sellableItemId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "quantity_value", nullable = false)),
        @AttributeOverride(name = "unit", column = @Column(name = "unit", nullable = false, length = 10))
    })
    private Quantity quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_price_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money unitPrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_cost_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money unitCost;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "line_total_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money lineTotal;

    @Column(name = "tax_class_code", length = 50)
    private String taxClassCode;

    @Column(name = "tax_rate_percent", precision = 5, scale = 2)
    private BigDecimal taxRatePercent;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "tax_amount_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money taxAmount;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "line_discount_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money lineDiscount;

    @Column(name = "override_reason")
    private String overrideReason;

    /**
     * Calculate line total from unit price and quantity.
     */
    public Money calculateLineTotal() {
        if (unitPrice == null || quantity == null) {
            return Money.zero(unitPrice != null ? unitPrice.getCurrency() : "UGX");
        }
        
        Money subtotal = unitPrice.multiply(quantity.getValue());
        
        if (lineDiscount != null && lineDiscount.isPositive()) {
            subtotal = subtotal.subtract(lineDiscount);
        }
        
        return subtotal;
    }

    /**
     * Calculate margin (selling price - cost).
     */
    public Money calculateMargin() {
        if (unitPrice == null || unitCost == null) {
            return Money.zero(unitPrice != null ? unitPrice.getCurrency() : "UGX");
        }
        
        Money marginPerUnit = unitPrice.subtract(unitCost);
        return marginPerUnit.multiply(quantity.getValue());
    }
}
