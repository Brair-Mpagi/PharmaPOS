package com.pharmapos.catalogue.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for price lists.
 */
@Data
public class PriceListRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    private Boolean isDefault = false;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    private LocalDate validTo;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency = "UGX";
}
