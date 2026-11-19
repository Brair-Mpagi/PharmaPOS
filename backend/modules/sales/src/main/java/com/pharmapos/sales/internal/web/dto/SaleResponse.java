package com.pharmapos.sales.internal.web.dto;

import com.pharmapos.sales.internal.domain.Sale;
import com.pharmapos.sales.internal.domain.SaleStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for sale data.
 */
public record SaleResponse(
    UUID id,
    String receiptNumber,
    UUID branchId,
    UUID shiftId,
    UUID cashierId,
    UUID customerId,
    SaleStatus status,
    Long subtotalAmount,
    Long taxAmount,
    Long totalAmount,
    Long paidAmount,
    Long changeAmount,
    Instant createdAt,
    Instant completedAt,
    String voidReason
) {
    public static SaleResponse fromDomain(Sale sale) {
        return new SaleResponse(
            sale.getId(),
            sale.getReceiptNumber(),
            sale.getBranchId(),
            sale.getShiftId(),
            sale.getCashierId(),
            sale.getCustomerId(),
            sale.getStatus(),
            sale.getSubtotalAmount(),
            sale.getTaxAmount(),
            sale.getTotalAmount(),
            sale.getPaidAmount(),
            sale.getChangeAmount(),
            sale.getCreatedAt(),
            sale.getCompletedAt(),
            sale.getVoidReason()
        );
    }
}
