package com.pharmapos.sales.internal.persistence;

import com.pharmapos.sales.internal.domain.Shift;
import com.pharmapos.sales.internal.domain.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Shift entity.
 * Manages shift lifecycle and queries for reconciliation.
 */
@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    /**
     * Find currently open shift for a cashier.
     * Business rule: Only one open shift per cashier at a time.
     */
    @Query("""
        SELECT s FROM Shift s
        WHERE s.cashierId = :cashierId
        AND s.status = 'OPEN'
        AND s.tenantId = :tenantId
        """)
    Optional<Shift> findOpenShiftByCashier(
        @Param("cashierId") UUID cashierId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find any open shift at a terminal.
     * Used to prevent multiple shifts on same terminal.
     */
    @Query("""
        SELECT s FROM Shift s
        WHERE s.terminalId = :terminalId
        AND s.status = 'OPEN'
        AND s.tenantId = :tenantId
        """)
    Optional<Shift> findOpenShiftByTerminal(
        @Param("terminalId") String terminalId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find shifts by branch and date range.
     */
    @Query("""
        SELECT s FROM Shift s
        WHERE s.branchId = :branchId
        AND s.openedAt >= :startDate
        AND s.openedAt <= :endDate
        AND s.tenantId = :tenantId
        ORDER BY s.openedAt DESC
        """)
    Page<Shift> findByBranchAndDateRange(
        @Param("branchId") UUID branchId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Find shifts by cashier.
     */
    @Query("""
        SELECT s FROM Shift s
        WHERE s.cashierId = :cashierId
        AND s.tenantId = :tenantId
        ORDER BY s.openedAt DESC
        """)
    Page<Shift> findByCashierId(
        @Param("cashierId") UUID cashierId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Find shifts with discrepancies (variance exceeds threshold).
     */
    @Query("""
        SELECT s FROM Shift s
        WHERE s.status = 'CLOSED'
        AND ABS(s.varianceAmount) > :thresholdMinorUnits
        AND s.branchId = :branchId
        AND s.tenantId = :tenantId
        ORDER BY s.closedAt DESC
        """)
    List<Shift> findShiftsWithDiscrepancies(
        @Param("branchId") UUID branchId,
        @Param("thresholdMinorUnits") long thresholdMinorUnits,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Check if cashier has any open shifts.
     */
    @Query("""
        SELECT COUNT(s) > 0
        FROM Shift s
        WHERE s.cashierId = :cashierId
        AND s.status = 'OPEN'
        AND s.tenantId = :tenantId
        """)
    boolean hasOpenShift(
        @Param("cashierId") UUID cashierId,
        @Param("tenantId") UUID tenantId
    );
}
