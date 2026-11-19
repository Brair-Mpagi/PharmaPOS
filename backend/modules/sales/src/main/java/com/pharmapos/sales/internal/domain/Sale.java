package com.pharmapos.sales.internal.domain;

import com.pharmapos.shared.domain.BranchEntity;
import com.pharmapos.shared.valueobject.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sale - Point of sale transaction.
 * Central aggregate for the sales module.
 */
@Entity
@Table(name = "sal_sale")
@Getter
@Setter
public class Sale extends BranchEntity {

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SaleStatus status = SaleStatus.DRAFT;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "total_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money total;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "tax_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money tax;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "discount_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money discount;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "saleId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<SaleLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "saleId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentNumber ASC")
    private List<Payment> payments = new ArrayList<>();

    public enum SaleStatus {
        DRAFT,      // Being created
        SUSPENDED,  // Saved for later
        COMPLETED,  // Finalized
        VOIDED      // Cancelled
    }

    /**
     * Add a sale line.
     */
    public void addLine(SaleLine line) {
        line.setSaleId(this.getId());
        line.setTenantId(this.getTenantId());
        line.setLineNumber(lines.size() + 1);
        lines.add(line);
    }

    /**
     * Add a payment.
     */
    public void addPayment(Payment payment) {
        payment.setSaleId(this.getId());
        payment.setTenantId(this.getTenantId());
        payment.setPaymentNumber(payments.size() + 1);
        payments.add(payment);
    }

    /**
     * Calculate total from lines.
     */
    public Money calculateTotal() {
        if (lines.isEmpty()) {
            return Money.zero(total != null ? total.getCurrency() : "UGX");
        }

        return lines.stream()
                .map(SaleLine::getLineTotal)
                .reduce(Money::add)
                .orElse(Money.zero(lines.get(0).getLineTotal().getCurrency()));
    }

    /**
     * Calculate total tax.
     */
    public Money calculateTotalTax() {
        if (lines.isEmpty()) {
            return Money.zero(total != null ? total.getCurrency() : "UGX");
        }

        return lines.stream()
                .map(SaleLine::getTaxAmount)
                .reduce(Money::add)
                .orElse(Money.zero(lines.get(0).getTaxAmount().getCurrency()));
    }

    /**
     * Calculate total payments.
     */
    public Money calculateTotalPayments() {
        if (payments.isEmpty()) {
            return Money.zero(total != null ? total.getCurrency() : "UGX");
        }

        return payments.stream()
                .filter(p -> p.getState() == Payment.PaymentState.CONFIRMED)
                .map(Payment::getAmount)
                .reduce(Money::add)
                .orElse(Money.zero(payments.get(0).getAmount().getCurrency()));
    }

    /**
     * Check if payments cover the total.
     */
    public boolean isFullyPaid() {
        Money totalPayments = calculateTotalPayments();
        return totalPayments.compareTo(total) >= 0;
    }

    /**
     * Complete the sale (finalize).
     */
    public void complete() {
        if (status != SaleStatus.DRAFT && status != SaleStatus.SUSPENDED) {
            throw new IllegalStateException("Sale cannot be completed from status: " + status);
        }

        if (!isFullyPaid()) {
            throw new IllegalStateException("Sale is not fully paid");
        }

        this.status = SaleStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Void the sale.
     */
    public void voidSale(UUID voidedBy, String reason) {
        if (status != SaleStatus.COMPLETED) {
            throw new IllegalStateException("Only completed sales can be voided");
        }

        this.status = SaleStatus.VOIDED;
        this.voidedAt = Instant.now();
        this.voidedBy = voidedBy;
        this.voidReason = reason;
    }

    /**
     * Suspend the sale (save for later).
     */
    public void suspend() {
        if (status != SaleStatus.DRAFT) {
            throw new IllegalStateException("Only draft sales can be suspended");
        }

        this.status = SaleStatus.SUSPENDED;
    }
}
