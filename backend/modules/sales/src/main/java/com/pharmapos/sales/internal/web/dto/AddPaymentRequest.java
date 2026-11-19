package com.pharmapos.sales.internal.web.dto;

import com.pharmapos.sales.internal.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request to add a payment to a sale.
 */
public record AddPaymentRequest(
    @NotNull(message = "Payment method is required")
    PaymentMethod method,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    Long amountMinorUnits,

    String transactionRef  // Required for MPESA/CARD, optional for CASH
) {}
