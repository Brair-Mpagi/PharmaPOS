package com.pharmapos.shared.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money value object - stores amounts as integer minor units.
 * 
 * Examples:
 * - USD 10.50 = Money(1050, "USD")
 * - UGX 5000 = Money(5000, "UGX") // No minor units in practice
 * 
 * This design eliminates floating-point rounding errors.
 * All arithmetic operations return new immutable instances.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class Money implements Serializable, Comparable<Money> {

    @Column(name = "amount_minor", nullable = false)
    private final long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private final String currency;

    private static final int MINOR_UNITS_USD = 2;
    private static final int MINOR_UNITS_UGX = 0; // Whole shillings

    /**
     * Constructor for JPA and Jackson.
     */
    @JsonCreator
    public Money(
            @JsonProperty("amountMinor") long amountMinor,
            @JsonProperty("currency") String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be 3-character ISO code");
        }
        this.amountMinor = amountMinor;
        this.currency = currency.toUpperCase();
    }

    /**
     * Default constructor for JPA.
     */
    protected Money() {
        this.amountMinor = 0;
        this.currency = "UGX";
    }

    /**
     * Create Money from major units (e.g., dollars, shillings).
     */
    public static Money fromMajor(BigDecimal major, String currency) {
        int minorUnits = getMinorUnits(currency);
        long amountMinor = major.multiply(BigDecimal.TEN.pow(minorUnits))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        return new Money(amountMinor, currency);
    }

    /**
     * Create Money from major units (double overload - use with caution).
     */
    public static Money fromMajor(double major, String currency) {
        return fromMajor(BigDecimal.valueOf(major), currency);
    }

    /**
     * Zero money in specified currency.
     */
    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    /**
     * Get amount in major units.
     */
    public BigDecimal toMajor() {
        int minorUnits = getMinorUnits(currency);
        return BigDecimal.valueOf(amountMinor)
                .divide(BigDecimal.TEN.pow(minorUnits), minorUnits, RoundingMode.HALF_UP);
    }

    /**
     * Add two money values (must be same currency).
     */
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amountMinor + other.amountMinor, this.currency);
    }

    /**
     * Subtract two money values (must be same currency).
     */
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amountMinor - other.amountMinor, this.currency);
    }

    /**
     * Multiply by a scalar.
     */
    public Money multiply(BigDecimal multiplier) {
        long result = BigDecimal.valueOf(amountMinor)
                .multiply(multiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        return new Money(result, currency);
    }

    /**
     * Multiply by a long.
     */
    public Money multiply(long multiplier) {
        return new Money(amountMinor * multiplier, currency);
    }

    /**
     * Divide by a scalar (banker's rounding).
     */
    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        long result = BigDecimal.valueOf(amountMinor)
                .divide(divisor, 0, RoundingMode.HALF_EVEN)
                .longValue();
        return new Money(result, currency);
    }

    /**
     * Negate the amount.
     */
    public Money negate() {
        return new Money(-amountMinor, currency);
    }

    /**
     * Absolute value.
     */
    public Money abs() {
        return amountMinor < 0 ? negate() : this;
    }

    /**
     * Check if amount is zero.
     */
    public boolean isZero() {
        return amountMinor == 0;
    }

    /**
     * Check if amount is positive.
     */
    public boolean isPositive() {
        return amountMinor > 0;
    }

    /**
     * Check if amount is negative.
     */
    public boolean isNegative() {
        return amountMinor < 0;
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return Long.compare(this.amountMinor, other.amountMinor);
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on different currencies: " + this.currency + " vs " + other.currency);
        }
    }

    private static int getMinorUnits(String currency) {
        return switch (currency.toUpperCase()) {
            case "UGX", "JPY", "KRW" -> 0;
            case "BHD", "KWD", "OMR" -> 3;
            default -> 2;
        };
    }
}
