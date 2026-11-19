package com.pharmapos.sales.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import com.pharmapos.shared.valueobject.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment - Payment for a sale.
 * Multiple payments per sale are supported (split payments).
 */
@Entity
@Table(name = "sal_payment")
@Getter
@Setter
public class Payment extends TenantEntity {

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Column(name = "method", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "amount_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money amount;

    @Column(name = "state", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentState state = PaymentState.PENDING;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_reason")
    private String reversalReason;

    public enum PaymentMethod {
        CASH,
        MOBILE_MONEY,
        CARD,
        CREDIT,
        BANK_TRANSFER
    }

    public enum PaymentState {
        PENDING,    // Awaiting confirmation
        CONFIRMED,  // Payment confirmed
        FAILED,     // Payment failed
        REVERSED    // Payment reversed/refunded
    }

    /**
     * Confirm the payment.
     */
    public void confirm() {
        if (state != PaymentState.PENDING) {
            throw new IllegalStateException("Only pending payments can be confirmed");
        }
        this.state = PaymentState.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    /**
     * Mark payment as failed.
     */
    public void fail() {
        if (state != PaymentState.PENDING) {
            throw new IllegalStateException("Only pending payments can be failed");
        }
        this.state = PaymentState.FAILED;
    }

    /**
     * Reverse the payment.
     */
    public void reverse(String reason) {
        if (state != PaymentState.CONFIRMED) {
            throw new IllegalStateException("Only confirmed payments can be reversed");
        }
        this.state = PaymentState.REVERSED;
        this.reversedAt = Instant.now();
        this.reversalReason = reason;
    }

    /**
     * Check if payment requires external confirmation.
     */
    public boolean requiresExternalConfirmation() {
        return method == PaymentMethod.MOBILE_MONEY || method == PaymentMethod.CARD;
    }
}
