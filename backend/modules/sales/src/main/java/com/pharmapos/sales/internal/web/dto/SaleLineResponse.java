package com.pharmapos.sales.internal.web.dto;

import com.pharmapos.sales.internal.domain.SaleLine;

import java.util.UUID;

/**
 * Response DTO for sale line data.
 */
public record SaleLineResponse(
    UUID id,
    UUID saleId,
    Integer lineNumber,
    UUID itemId,
    Integer quantity,
    Long unitPrice,
    Long taxAmount,
    Long lineTotal
) {
    public static SaleLineResponse fromDomain(SaleLine line) {
        return new SaleLineResponse(
            line.getId(),
            line.getSaleId(),
            line.getLineNumber(),
            line.getItemId(),
            line.getQuantity(),
            line.getUnitPrice(),
            line.getTaxAmount(),
            line.getLineTotal()
        );
    }
}
