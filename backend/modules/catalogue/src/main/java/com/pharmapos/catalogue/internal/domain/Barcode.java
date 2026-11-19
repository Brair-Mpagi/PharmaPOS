package com.pharmapos.catalogue.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Barcode - Multiple barcodes can be associated with a sellable item.
 */
@Entity
@Table(name = "cat_barcode")
@Getter
@Setter
public class Barcode extends TenantEntity {

    @Column(name = "sellable_item_id", nullable = false)
    private UUID sellableItemId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BarcodeType type;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public enum BarcodeType {
        GTIN,      // Global Trade Item Number
        EAN13,     // European Article Number (13 digits)
        EAN8,      // European Article Number (8 digits)
        UPC,       // Universal Product Code
        CODE128,   // Code 128 barcode
        INTERNAL   // Pharmacy-internal code
    }

    /**
     * Validate barcode format based on type.
     */
    public boolean isValidFormat() {
        if (code == null || code.isBlank()) {
            return false;
        }

        return switch (type) {
            case EAN13 -> code.matches("\\d{13}");
            case EAN8 -> code.matches("\\d{8}");
            case UPC -> code.matches("\\d{12}");
            case GTIN -> code.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}");
            case CODE128, INTERNAL -> code.length() >= 3 && code.length() <= 100;
        };
    }
}
