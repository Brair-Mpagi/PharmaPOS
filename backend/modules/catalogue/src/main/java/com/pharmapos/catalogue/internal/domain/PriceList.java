package com.pharmapos.catalogue.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Price List - Named pricing strategy with validity period.
 */
@Entity
@Table(name = "cat_price_list")
@Getter
@Setter
public class PriceList extends TenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "UGX";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Check if price list is valid on a given date.
     */
    public boolean isValidOn(LocalDate date) {
        if (!isActive || date == null) {
            return false;
        }
        
        boolean afterStart = !date.isBefore(validFrom);
        boolean beforeEnd = validTo == null || !date.isAfter(validTo);
        
        return afterStart && beforeEnd;
    }

    /**
     * Check if price list is currently valid.
     */
    public boolean isCurrentlyValid() {
        return isValidOn(LocalDate.now());
    }
}
