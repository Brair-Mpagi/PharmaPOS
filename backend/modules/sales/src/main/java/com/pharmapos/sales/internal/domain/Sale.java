package com.pharmapos.sales.internal.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity mapping to {@code sal_sale}.
 * <p>
 * Represents a point-of-sale transaction in a pharmacy branch.
 * Each sale belongs to exactly one tenant, branch, shift and device.
 * Lines and payments are eagerly cascaded but lazily fetched.
 */
@Entity
@Table(name = "sal_sale")
public class Sale {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", updatable = false, nullable = false)
    private UUID branchId;

    @Column(name = "shift_id", updatable = false, nullable = false)
    private UUID shiftId;

    @Column(name = "device_id", updatable = false, nullable = false)
    private UUID deviceId;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "idempotency_key", updatable = false, nullable = false)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaleStatus status;

    @Column(name = "total_minor", nullable = false)
    private long totalMinor;

    @Column(name = "tax_minor", nullable = false)
    private long taxMinor;

    @Column(name = "discount_minor", nullable = false)
    private long discountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false, nullable = false)
    private UUID createdBy;

    @Version
    @Column(name = "version")
    private int version;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<SaleLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Payment> payments = new ArrayList<>();

    protected Sale() {
        // JPA
    }

    /**
     * Factory for creating a new DRAFT sale.
     */
    public static Sale createDraft(UUID id, UUID tenantId, UUID branchId, UUID shiftId,
                                   UUID deviceId, UUID idempotencyKey, String currency,
                                   UUID createdBy) {
        Sale sale = new Sale();
        sale.id = Objects.requireNonNull(id, "id");
        sale.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        sale.branchId = Objects.requireNonNull(branchId, "branchId");
        sale.shiftId = Objects.requireNonNull(shiftId, "shiftId");
        sale.deviceId = Objects.requireNonNull(deviceId, "deviceId");
        sale.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        sale.currency = Objects.requireNonNull(currency, "currency");
        sale.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        sale.status = SaleStatus.DRAFT;
        sale.totalMinor = 0L;
        sale.taxMinor = 0L;
        sale.discountMinor = 0L;
        sale.createdAt = Instant.now();
        return sale;
    }

    // ── Mutation methods ────────────────────────────────────────────

    public void addLine(SaleLine line) {
        Objects.requireNonNull(line, "line must not be null");
        line.setSale(this);
        lines.add(line);
        recalculateTotals();
    }

    public void addPayment(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        payment.setSale(this);
        payments.add(payment);
    }

    /**
     * Finalise the sale: set receipt number, transition to COMPLETED.
     * Must be called within the finalise transaction after all validations pass.
     */
    public void finalise(String receiptNumber) {
        if (status != SaleStatus.DRAFT) {
            throw new IllegalStateException(
                    "Can only finalise a DRAFT sale; current status is " + status);
        }
        recalculateTotals();
        this.receiptNumber = Objects.requireNonNull(receiptNumber, "receiptNumber");
        this.status = SaleStatus.COMPLETED;
    }

    /**
     * Suspend the sale for later retrieval.
     */
    public void suspend() {
        if (status != SaleStatus.DRAFT) {
            throw new IllegalStateException(
                    "Can only suspend a DRAFT sale; current status is " + status);
        }
        this.status = SaleStatus.SUSPENDED;
    }

    /**
     * Resume a previously suspended sale back to DRAFT.
     */
    public void resume() {
        if (status != SaleStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Can only resume a SUSPENDED sale; current status is " + status);
        }
        this.status = SaleStatus.DRAFT;
    }

    /**
     * Void a sale. Only DRAFT or SUSPENDED sales may be voided.
     */
    public void voidSale() {
        if (status != SaleStatus.DRAFT && status != SaleStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Cannot void a sale with status " + status);
        }
        this.status = SaleStatus.VOIDED;
    }

    /**
     * Recalculates total, tax and discount from lines.
     */
    public void recalculateTotals() {
        long total = 0L;
        long tax = 0L;
        long discount = 0L;
        for (SaleLine line : lines) {
            long lineGross = line.getUnitPriceMinor() * line.getQuantity().longValue();
            total += lineGross - line.getLineDiscountMinor();
            discount += line.getLineDiscountMinor();
            // Tax is calculated per tax class percentage applied to line total
            tax += line.computeTaxMinor();
        }
        this.totalMinor = total;
        this.taxMinor = tax;
        this.discountMinor = discount;
    }

    /**
     * Returns the total payment amount already confirmed.
     */
    public long confirmedPaymentTotal() {
        return payments.stream()
                .filter(p -> p.getState() == PaymentState.CONFIRMED)
                .mapToLong(Payment::getAmountMinor)
                .sum();
    }

    /**
     * Returns total of all non-failed payments (PENDING + CONFIRMED).
     */
    public long totalPaymentAmount() {
        return payments.stream()
                .filter(p -> p.getState() != PaymentState.FAILED && p.getState() != PaymentState.REVERSED)
                .mapToLong(Payment::getAmountMinor)
                .sum();
    }

    // ── Getters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getBranchId() { return branchId; }
    public UUID getShiftId() { return shiftId; }
    public UUID getDeviceId() { return deviceId; }
    public String getReceiptNumber() { return receiptNumber; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public SaleStatus getStatus() { return status; }
    public long getTotalMinor() { return totalMinor; }
    public long getTaxMinor() { return taxMinor; }
    public long getDiscountMinor() { return discountMinor; }
    public String getCurrency() { return currency; }
    public UUID getCustomerId() { return customerId; }
    public UUID getPrescriptionId() { return prescriptionId; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public int getVersion() { return version; }
    public List<SaleLine> getLines() { return List.copyOf(lines); }
    public List<Payment> getPayments() { return List.copyOf(payments); }

    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public void setPrescriptionId(UUID prescriptionId) { this.prescriptionId = prescriptionId; }
}
