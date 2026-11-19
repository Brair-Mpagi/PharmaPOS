package com.pharmapos.catalogue.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tax Class - Defines tax rates for items (VAT, GST, etc.).
 */
@Entity
@Table(name = "cat_tax_class")
@Getter
@Setter
public class TaxClass extends TenantEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Calculate tax amount for a given base amount.
     */
    public BigDecimal calculateTaxAmount(BigDecimal baseAmount) {
        if (baseAmount == null || ratePercent == null) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(ratePercent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate total including tax.
     */
    public BigDecimal calculateTotalWithTax(BigDecimal baseAmount) {
        if (baseAmount == null) {
            return BigDecimal.ZERO;
        }
        return baseAmount.add(calculateTaxAmount(baseAmount));
    }
}
