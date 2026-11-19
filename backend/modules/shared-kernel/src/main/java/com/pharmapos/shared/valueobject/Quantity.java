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
 * Quantity value object with unit of measure.
 * All quantities are stored in base units (e.g., TAB, ML, G).
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class Quantity implements Serializable, Comparable<Quantity> {

    @Column(name = "quantity_value", nullable = false, precision = 14, scale = 3)
    private final BigDecimal value;

    @Column(name = "quantity_unit", nullable = false, length = 10)
    private final String unit;

    @JsonCreator
    public Quantity(
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("unit") String unit) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity value must be non-negative");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Unit must not be blank");
        }
        this.value = value.setScale(3, RoundingMode.HALF_UP);
        this.unit = unit.toUpperCase();
    }

    /**
     * Default constructor for JPA.
     */
    protected Quantity() {
        this.value = BigDecimal.ZERO;
        this.unit = "EA";
    }

    /**
     * Create quantity from double (use with caution).
     */
    public static Quantity of(double value, String unit) {
        return new Quantity(BigDecimal.valueOf(value), unit);
    }

    /**
     * Zero quantity in specified unit.
     */
    public static Quantity zero(String unit) {
        return new Quantity(BigDecimal.ZERO, unit);
    }

    /**
     * Add quantities (must be same unit).
     */
    public Quantity add(Quantity other) {
        assertSameUnit(other);
        return new Quantity(this.value.add(other.value), this.unit);
    }

    /**
     * Subtract quantities (must be same unit).
     */
    public Quantity subtract(Quantity other) {
        assertSameUnit(other);
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        return new Quantity(result, this.unit);
    }

    /**
     * Multiply by a scalar.
     */
    public Quantity multiply(BigDecimal multiplier) {
        return new Quantity(value.multiply(multiplier), unit);
    }

    /**
     * Multiply by a long.
     */
    public Quantity multiply(long multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Check if quantity is zero.
     */
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if quantity is positive.
     */
    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public int compareTo(Quantity other) {
        assertSameUnit(other);
        return this.value.compareTo(other.value);
    }

    private void assertSameUnit(Quantity other) {
        if (!this.unit.equals(other.unit)) {
            throw new IllegalArgumentException(
                    "Cannot operate on different units: " + this.unit + " vs " + other.unit);
        }
    }
}
