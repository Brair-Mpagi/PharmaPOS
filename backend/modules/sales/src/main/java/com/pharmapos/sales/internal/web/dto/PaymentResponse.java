package com.pharmapos.sales.internal.web.dto;

import com.pharmapos.sales.internal.domain.Payment;
import com.pharmapos.sales.internal.domain.PaymentMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment data.
 */
public record PaymentResponse(
    UUID id,
    UUID saleId,
    PaymentMethod method,
    Long amount,
    String transactionRef,
    Instant createdAt
) {
    public static PaymentResponse fromDomain(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getSaleId(),
            payment.getMethod(),
            payment.getAmount(),
            payment.getTransactionRef(),
            payment.getCreatedAt()
        );
    }
}
