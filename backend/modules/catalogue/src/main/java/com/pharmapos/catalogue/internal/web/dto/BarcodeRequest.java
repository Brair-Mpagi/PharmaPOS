package com.pharmapos.catalogue.internal.web.dto;

import com.pharmapos.catalogue.internal.domain.Barcode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for adding barcodes.
 */
@Data
public class BarcodeRequest {

    @NotBlank(message = "Barcode code is required")
    private String code;

    @NotNull(message = "Barcode type is required")
    private Barcode.BarcodeType type;

    private Boolean isPrimary = false;
}
