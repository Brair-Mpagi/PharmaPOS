package com.pharmapos.sales.internal.domain;

import com.pharmapos.shared.domain.BranchEntity;
import com.pharmapos.shared.valueobject.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Shift - Cashier session for cash reconciliation.
 */
@Entity
@Table(name = "sal_shift")
@Getter
@Setter
public class Shift extends BranchEntity {

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "shift_number", nullable = false, length = 50)
    private String shiftNumber;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "opening_cash_minor", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money openingCash;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "closing_cash_minor")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money closingCash;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "expected_cash_minor")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money expectedCash;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "variance_minor")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, insertable = false, updatable = false))
    })
    private Money variance;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ShiftStatus {
        OPEN,
        CLOSED
    }

    /**
     * Close the shift.
     */
    public void close(UUID closedBy, Money closingCash, Money expectedCash) {
        if (status != ShiftStatus.OPEN) {
            throw new IllegalStateException("Shift is already closed");
        }

        this.status = ShiftStatus.CLOSED;
        this.closedAt = Instant.now();
        this.closedBy = closedBy;
        this.closingCash = closingCash;
        this.expectedCash = expectedCash;
        
        // Calculate variance
        if (closingCash != null && expectedCash != null) {
            this.variance = closingCash.subtract(expectedCash);
        }
    }

    /**
     * Check if shift has variance issues.
     */
    public boolean hasVariance() {
        return variance != null && !variance.isZero();
    }

    /**
     * Check if variance is significant (more than acceptable threshold).
     */
    public boolean hasSignificantVariance(Money threshold) {
        if (variance == null) {
            return false;
        }
        return variance.abs().compareTo(threshold) > 0;
    }
}
