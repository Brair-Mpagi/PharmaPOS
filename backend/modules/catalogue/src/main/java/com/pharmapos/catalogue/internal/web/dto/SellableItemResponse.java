package com.pharmapos.catalogue.internal.web.dto;

import com.pharmapos.catalogue.internal.domain.SellableItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for sellable items.
 */
@Data
@Builder
public class SellableItemResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String genericName;
    private String activeIngredient;
    private String strength;
    private String dosageForm;
    private String baseUnit;
    private BigDecimal packSize;
    private String manufacturer;
    private boolean requiresPrescription;
    private boolean isControlled;
    private String schedule;
    private String status;
    private String description;
    private String storageConditions;
    private UUID taxClassId;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public static SellableItemResponse from(SellableItem item) {
        return SellableItemResponse.builder()
                .id(item.getId())
                .tenantId(item.getTenantId())
                .name(item.getName())
                .genericName(item.getGenericName())
                .activeIngredient(item.getActiveIngredient())
                .strength(item.getStrength())
                .dosageForm(item.getDosageForm())
                .baseUnit(item.getBaseUnit())
                .packSize(item.getPackSize())
                .manufacturer(item.getManufacturer())
                .requiresPrescription(item.isRequiresPrescription())
                .isControlled(item.isControlled())
                .schedule(item.getSchedule() != null ? item.getSchedule().name() : null)
                .status(item.getStatus().name())
                .description(item.getDescription())
                .storageConditions(item.getStorageConditions())
                .taxClassId(item.getTaxClassId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .version(item.getVersion())
                .build();
    }
}
