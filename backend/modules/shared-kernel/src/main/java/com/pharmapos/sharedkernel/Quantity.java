package com.pharmapos.sharedkernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable quantity value object representing an amount in a specific unit.
 *
 * <p>Quantities are stored using {@link BigDecimal} with a scale of 3
 * (matching the database {@code numeric(14,3)} type) to support millilitre-level
 * dispensing precision.
 */
public final class Quantity {

    /** Standard math context: 14-digit precision, half-up rounding. */
    private static final MathContext MC = new MathContext(14, RoundingMode.HALF_UP);
    private static final int SCALE = 3;

    private final BigDecimal amount;
    private final String unit;

    @JsonCreator
    public Quantity(
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("unit") String unit) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit must not be blank");
        }
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        this.unit = unit;
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    public static Quantity of(BigDecimal amount, String unit) {
        return new Quantity(amount, unit);
    }

    public static Quantity of(long amount, String unit) {
        return new Quantity(BigDecimal.valueOf(amount), unit);
    }

    public static Quantity of(double amount, String unit) {
        return new Quantity(BigDecimal.valueOf(amount), unit);
    }

    public static Quantity zero(String unit) {
        return new Quantity(BigDecimal.ZERO, unit);
    }

    // -------------------------------------------------------------------------
    // Arithmetic — all guard against unit mismatch
    // -------------------------------------------------------------------------

    public Quantity add(Quantity other) {
        assertSameUnit(other);
        return new Quantity(this.amount.add(other.amount, MC), this.unit);
    }

    public Quantity subtract(Quantity other) {
        assertSameUnit(other);
        return new Quantity(this.amount.subtract(other.amount, MC), this.unit);
    }

    public Quantity multiply(int factor) {
        return new Quantity(this.amount.multiply(BigDecimal.valueOf(factor), MC), this.unit);
    }

    public Quantity multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Quantity(this.amount.multiply(factor, MC), this.unit);
    }

    public Quantity negate() {
        return new Quantity(this.amount.negate(MC), this.unit);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isGreaterThanOrEqualTo(Quantity other) {
        assertSameUnit(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Quantity other) {
        assertSameUnit(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Returns the smaller of this and the other quantity (same unit required).
     */
    public Quantity min(Quantity other) {
        assertSameUnit(other);
        return this.amount.compareTo(other.amount) <= 0 ? this : other;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty("unit")
    public String getUnit() {
        return unit;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void assertSameUnit(Quantity other) {
        Objects.requireNonNull(other, "other Quantity must not be null");
        if (!this.unit.equals(other.unit)) {
            throw new UnitMismatchException(
                    String.format("Cannot operate on '%s' and '%s'", this.unit, other.unit));
        }
    }

    // -------------------------------------------------------------------------
    // Object contract
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity q)) return false;
        return amount.compareTo(q.amount) == 0 && unit.equals(q.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), unit);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + unit;
    }

    // -------------------------------------------------------------------------
    // Nested exception
    // -------------------------------------------------------------------------

    public static final class UnitMismatchException extends IllegalArgumentException {
        public UnitMismatchException(String message) {
            super(message);
        }
    }
}
