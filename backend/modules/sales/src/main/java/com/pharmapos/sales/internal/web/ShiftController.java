package com.pharmapos.sales.internal.web;

import com.pharmapos.sales.internal.application.ShiftService;
import com.pharmapos.sales.internal.domain.Shift;
import com.pharmapos.sales.internal.web.dto.ShiftCloseRequest;
import com.pharmapos.sales.internal.web.dto.ShiftOpenRequest;
import com.pharmapos.sales.internal.web.dto.ShiftResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST API controller for shift management.
 * Exposes endpoints for shift lifecycle: open, close, query.
 */
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    /**
     * POST /api/v1/shifts
     * Open a new shift.
     */
    @PostMapping
    public ResponseEntity<ShiftResponse> openShift(@Valid @RequestBody ShiftOpenRequest request) {
        Shift shift = shiftService.openShift(
            request.branchId(),
            request.cashierId(),
            request.terminalId(),
            request.openingCashMinorUnits()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ShiftResponse.fromDomain(shift));
    }

    /**
     * POST /api/v1/shifts/{id}/close
     * Close a shift with reconciliation.
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<ShiftResponse> closeShift(
        @PathVariable UUID id,
        @Valid @RequestBody ShiftCloseRequest request
    ) {
        Shift shift = shiftService.closeShift(
            id,
            request.actualCashMinorUnits(),
            request.closureNotes()
        );
        return ResponseEntity.ok(ShiftResponse.fromDomain(shift));
    }

    /**
     * GET /api/v1/shifts/{id}
     * Get shift by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> getShift(@PathVariable UUID id) {
        return shiftService.findById(id)
            .map(ShiftResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/shifts/cashier/{cashierId}/current
     * Get currently open shift for a cashier.
     */
    @GetMapping("/cashier/{cashierId}/current")
    public ResponseEntity<ShiftResponse> getCurrentShift(@PathVariable UUID cashierId) {
        return shiftService.findOpenShiftForCashier(cashierId)
            .map(ShiftResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/shifts/cashier/{cashierId}/has-open
     * Check if cashier has an open shift.
     */
    @GetMapping("/cashier/{cashierId}/has-open")
    public ResponseEntity<Boolean> hasOpenShift(@PathVariable UUID cashierId) {
        boolean hasOpen = shiftService.hasOpenShift(cashierId);
        return ResponseEntity.ok(hasOpen);
    }

    /**
     * GET /api/v1/shifts/branch/{branchId}
     * List shifts by branch and date range.
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Page<ShiftResponse>> getShiftsByBranch(
        @PathVariable UUID branchId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        Pageable pageable
    ) {
        Page<Shift> shifts = shiftService.findByBranchAndDateRange(
            branchId, startDate, endDate, pageable
        );
        return ResponseEntity.ok(shifts.map(ShiftResponse::fromDomain));
    }

    /**
     * GET /api/v1/shifts/cashier/{cashierId}
     * List all shifts for a cashier.
     */
    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<Page<ShiftResponse>> getShiftsByCashier(
        @PathVariable UUID cashierId,
        Pageable pageable
    ) {
        Page<Shift> shifts = shiftService.findByCashier(cashierId, pageable);
        return ResponseEntity.ok(shifts.map(ShiftResponse::fromDomain));
    }
}
