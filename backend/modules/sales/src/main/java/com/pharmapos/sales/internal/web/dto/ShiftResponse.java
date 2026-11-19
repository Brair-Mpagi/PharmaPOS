package com.pharmapos.sales.internal.web.dto;

import com.pharmapos.sales.internal.domain.Shift;
import com.pharmapos.sales.internal.domain.ShiftStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for shift data.
 */
public record ShiftResponse(
    UUID id,
    UUID branchId,
    UUID cashierId,
    String terminalId,
    ShiftStatus status,
    Long openingCashMinorUnits,
    Long expectedCashMinorUnits,
    Long actualCashMinorUnits,
    Long varianceMinorUnits,
    Long totalSalesAmount,
    Long saleCount,
    Instant openedAt,
    Instant closedAt,
    String closureNotes
) {
    public static ShiftResponse fromDomain(Shift shift) {
        return new ShiftResponse(
            shift.getId(),
            shift.getBranchId(),
            shift.getCashierId(),
            shift.getTerminalId(),
            shift.getStatus(),
            shift.getOpeningCashMinorUnits(),
            shift.getExpectedCashMinorUnits(),
            shift.getActualCashMinorUnits(),
            shift.getVarianceAmount(),
            shift.getTotalSalesAmount(),
            shift.getSaleCount(),
            shift.getOpenedAt(),
            shift.getClosedAt(),
            shift.getClosureNotes()
        );
    }
}
