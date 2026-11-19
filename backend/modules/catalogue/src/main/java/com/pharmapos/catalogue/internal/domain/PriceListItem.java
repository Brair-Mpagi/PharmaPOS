package com.pharmapos.catalogue.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import com.pharmapos.shared.valueobject.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Price List Item - Individual item price within a price list.
 * Supports quantity-based pricing (price breaks).
 */
@Entity
@Table(name = "cat_price_list_item")
@Getter
@Setter
public class PriceListItem extends TenantEntity {

    @Column(name = "price_list_id", nullable = false)
    private UUID priceListId;

    @Column(name = "sellable_item_id", nullable = false)
    private UUID sellableItemId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_price_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money unitPrice;

    @Column(name = "min_quantity", precision = 10, scale = 2)
    private BigDecimal minQuantity;

    @Column(name = "max_quantity", precision = 10, scale = 2)
    private BigDecimal maxQuantity;

    /**
     * Check if this price applies to a given quantity.
     */
    public boolean appliesTo(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        boolean meetsMin = minQuantity == null || quantity.compareTo(minQuantity) >= 0;
        boolean belowMax = maxQuantity == null || quantity.compareTo(maxQuantity) <= 0;

        return meetsMin && belowMax;
    }

    /**
     * Calculate line total for a given quantity.
     */
    public Money calculateLineTotal(BigDecimal quantity) {
        if (unitPrice == null || quantity == null) {
            return Money.zero(unitPrice != null ? unitPrice.getCurrency() : "UGX");
        }
        return unitPrice.multiply(quantity);
    }
}
