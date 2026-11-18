package com.pharmapos.sales.internal.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object for sequential receipt numbers scoped to a device within a branch.
 * <p>
 * Format: {@code BRANCH_SHORT-DEVICE_SHORT-SEQ} e.g. {@code BR01-D03-000457}.
 * The sequence is maintained via a database counter per (tenant_id, branch_id, device_id) tuple,
 * incremented inside the sale-finalisation transaction to guarantee uniqueness.
 */
public record ReceiptNumber(UUID tenantId, UUID branchId, UUID deviceId, long sequence) {

    public ReceiptNumber {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        if (sequence < 1) {
            throw new IllegalArgumentException("Receipt sequence must be >= 1, got " + sequence);
        }
    }

    /**
     * Formats the receipt number for printing.
     * Uses the lower 4 hex digits of branch and device UUIDs for compactness.
     */
    public String formatted() {
        String branchShort = branchId.toString().substring(branchId.toString().length() - 4).toUpperCase();
        String deviceShort = deviceId.toString().substring(deviceId.toString().length() - 4).toUpperCase();
        return String.format("%s-%s-%06d", branchShort, deviceShort, sequence);
    }

    @Override
    public String toString() {
        return formatted();
    }
}
