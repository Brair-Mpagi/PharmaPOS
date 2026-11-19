package com.pharmapos.sales.internal.application;

import com.pharmapos.sales.internal.domain.Shift;
import com.pharmapos.sales.internal.domain.ShiftStatus;
import com.pharmapos.sales.internal.persistence.PaymentRepository;
import com.pharmapos.sales.internal.persistence.SaleRepository;
import com.pharmapos.sales.internal.persistence.ShiftRepository;
import com.pharmapos.sharedkernel.TenantContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for shift management.
 * Handles shift lifecycle: open, close, reconciliation.
 */
@Service
@Transactional
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;

    public ShiftService(
        ShiftRepository shiftRepository,
        SaleRepository saleRepository,
        PaymentRepository paymentRepository
    ) {
        this.shiftRepository = shiftRepository;
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Open a new shift.
     * Validates: no existing open shift for cashier or terminal.
     */
    public Shift openShift(
        UUID branchId,
        UUID cashierId,
        String terminalId,
        long openingCashMinorUnits
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();

        // Validate no open shift for this cashier
        Optional<Shift> existingCashierShift = shiftRepository
            .findOpenShiftByCashier(cashierId, tenantId);
        if (existingCashierShift.isPresent()) {
            throw new IllegalStateException(
                "Cashier already has an open shift: " + existingCashierShift.get().getId()
            );
        }

        // Validate no open shift on this terminal
        Optional<Shift> existingTerminalShift = shiftRepository
            .findOpenShiftByTerminal(terminalId, tenantId);
        if (existingTerminalShift.isPresent()) {
            throw new IllegalStateException(
                "Terminal already has an open shift: " + existingTerminalShift.get().getId()
            );
        }

        // Create and save new shift
        Shift shift = new Shift(
            UUID.randomUUID(),
            tenantId,
            branchId,
            cashierId,
            terminalId,
            openingCashMinorUnits
        );

        return shiftRepository.save(shift);
    }

    /**
     * Close a shift with reconciliation.
     * Calculates expected vs actual cash and variance.
     */
    public Shift closeShift(
        UUID shiftId,
        long actualCashMinorUnits,
        String closureNotes
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();

        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        // Verify tenant ownership
        if (!shift.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Shift does not belong to current tenant");
        }

        // Calculate totals from sales and payments
        long totalSalesAmount = saleRepository.calculateShiftTotal(shiftId, tenantId);
        long saleCount = saleRepository.countByShift(shiftId, tenantId);

        // Close shift with reconciliation
        shift.close(actualCashMinorUnits, totalSalesAmount, saleCount, closureNotes);

        return shiftRepository.save(shift);
    }

    /**
     * Get currently open shift for a cashier.
     */
    @Transactional(readOnly = true)
    public Optional<Shift> findOpenShiftForCashier(UUID cashierId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return shiftRepository.findOpenShiftByCashier(cashierId, tenantId);
    }

    /**
     * Get shift by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Shift> findById(UUID shiftId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return shiftRepository.findById(shiftId)
            .filter(shift -> shift.getTenantId().equals(tenantId));
    }

    /**
     * List shifts by branch and date range.
     */
    @Transactional(readOnly = true)
    public Page<Shift> findByBranchAndDateRange(
        UUID branchId,
        Instant startDate,
        Instant endDate,
        Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return shiftRepository.findByBranchAndDateRange(
            branchId, startDate, endDate, tenantId, pageable
        );
    }

    /**
     * List shifts by cashier.
     */
    @Transactional(readOnly = true)
    public Page<Shift> findByCashier(UUID cashierId, Pageable pageable) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return shiftRepository.findByCashierId(cashierId, tenantId, pageable);
    }

    /**
     * Check if cashier has open shift.
     */
    @Transactional(readOnly = true)
    public boolean hasOpenShift(UUID cashierId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return shiftRepository.hasOpenShift(cashierId, tenantId);
    }
}
