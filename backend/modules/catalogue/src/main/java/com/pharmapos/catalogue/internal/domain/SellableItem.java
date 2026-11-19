package com.pharmapos.catalogue.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sellable Item - A product that can be sold in the pharmacy.
 * Combines product definition and SKU concept.
 */
@Entity
@Table(name = "cat_sellable_item")
@Getter
@Setter
public class SellableItem extends TenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "active_ingredient", length = 500)
    private String activeIngredient;

    @Column(name = "strength", length = 100)
    private String strength;

    @Column(name = "dosage_form", length = 50)
    private String dosageForm;

    @Column(name = "base_unit", nullable = false, length = 10)
    private String baseUnit;

    @Column(name = "pack_size", precision = 10, scale = 2)
    private BigDecimal packSize;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription = false;

    @Column(name = "is_controlled", nullable = false)
    private boolean isControlled = false;

    @Column(name = "schedule", length = 20)
    @Enumerated(EnumType.STRING)
    private DrugSchedule schedule;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "storage_conditions")
    private String storageConditions;

    @Column(name = "tax_class_id")
    private UUID taxClassId;

    public enum ItemStatus {
        ACTIVE,
        DISCONTINUED,
        SUSPENDED
    }

    public enum DrugSchedule {
        II,   // High potential for abuse
        III,  // Moderate to low potential for physical dependence
        IV,   // Low potential for abuse
        V     // Lower potential for abuse than Schedule IV
    }

    /**
     * Check if item is available for sale.
     */
    public boolean isAvailable() {
        return status == ItemStatus.ACTIVE;
    }

    /**
     * Check if item requires special handling.
     */
    public boolean requiresSpecialHandling() {
        return isControlled || requiresPrescription;
    }
}
