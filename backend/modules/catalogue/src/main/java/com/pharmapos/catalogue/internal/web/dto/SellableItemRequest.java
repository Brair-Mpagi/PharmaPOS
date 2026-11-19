package com.pharmapos.catalogue.internal.web.dto;

import com.pharmapos.catalogue.internal.domain.SellableItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating/updating sellable items.
 */
@Data
public class SellableItemRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String genericName;

    @Size(max = 500, message = "Active ingredient must not exceed 500 characters")
    private String activeIngredient;

    @Size(max = 100, message = "Strength must not exceed 100 characters")
    private String strength;

    @Size(max = 50, message = "Dosage form must not exceed 50 characters")
    private String dosageForm;

    @NotBlank(message = "Base unit is required")
    @Size(max = 10, message = "Base unit must not exceed 10 characters")
    private String baseUnit;

    private BigDecimal packSize;

    @Size(max = 255, message = "Manufacturer must not exceed 255 characters")
    private String manufacturer;

    @NotNull(message = "Requires prescription flag is required")
    private Boolean requiresPrescription;

    @NotNull(message = "Is controlled flag is required")
    private Boolean isControlled;

    private SellableItem.DrugSchedule schedule;

    private String description;

    @Size(max = 255, message = "Storage conditions must not exceed 255 characters")
    private String storageConditions;

    private UUID taxClassId;
}
