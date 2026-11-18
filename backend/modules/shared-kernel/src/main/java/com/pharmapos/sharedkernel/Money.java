package com.pharmapos.sharedkernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Immutable monetary value object.
 *
 * <p>Money is stored as an integer number of minor units plus a 3-character ISO 4217 currency code.
 * For UGX (Ugandan shillings) there are no minor units in practice, so the amount in minor units
 * equals the amount in whole shillings.
 *
 * <p>All arithmetic operations guard against currency mismatch and return a new Money instance.
 * JSON serialisation format: {@code {"amountMinor": 12500, "currency": "UGX"}}.
 */
public final class Money {

    private final long amountMinor;
    private final String currency;

    @JsonCreator
    public Money(
            @JsonProperty("amountMinor") long amountMinor,
            @JsonProperty("currency") String currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-character ISO 4217 code, got: " + currency);
        }
        this.amountMinor = amountMinor;
        this.currency = currency.toUpperCase();
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    public static Money of(long amountMinor, String currency) {
        return new Money(amountMinor, currency);
    }

    public static Money zero(String currency) {
        return new Money(0L, currency);
    }

    // -------------------------------------------------------------------------
    // Arithmetic — all guard against currency mismatch
    // -------------------------------------------------------------------------

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(Math.addExact(this.amountMinor, other.amountMinor), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(Math.subtractExact(this.amountMinor, other.amountMinor), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(Math.multiplyExact(this.amountMinor, factor), this.currency);
    }

    public Money multiply(long factor) {
        return new Money(Math.multiplyExact(this.amountMinor, factor), this.currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(this.amountMinor), this.currency);
    }

    public boolean isNegative() {
        return amountMinor < 0;
    }

    public boolean isZero() {
        return amountMinor == 0;
    }

    public boolean isPositive() {
        return amountMinor > 0;
    }

    /**
     * Returns true if this amount is greater than the given amount (same currency required).
     */
    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amountMinor > other.amountMinor;
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.amountMinor < other.amountMinor;
    }

    // -------------------------------------------------------------------------
    // Accessors (also used for JSON serialisation)
    // -------------------------------------------------------------------------

    @JsonProperty("amountMinor")
    public long getAmountMinor() {
        return amountMinor;
    }

    @JsonProperty("currency")
    public String getCurrency() {
        return currency;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "other Money must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                    String.format("Cannot operate on %s and %s", this.currency, other.currency));
        }
    }

    // -------------------------------------------------------------------------
    // Object contract
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amountMinor == money.amountMinor && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountMinor, currency);
    }

    @Override
    public String toString() {
        return amountMinor + " " + currency;
    }

    // -------------------------------------------------------------------------
    // Nested exception
    // -------------------------------------------------------------------------

    public static final class CurrencyMismatchException extends IllegalArgumentException {
        public CurrencyMismatchException(String message) {
            super(message);
        }
    }
}
