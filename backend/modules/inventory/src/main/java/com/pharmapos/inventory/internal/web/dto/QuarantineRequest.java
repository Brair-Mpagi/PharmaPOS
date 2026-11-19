package com.pharmapos.inventory.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for quarantining a batch.
 */
@Data
public class QuarantineRequest {

    @NotBlank(message = "Quarantine reason is required")
    private String reason;
}
