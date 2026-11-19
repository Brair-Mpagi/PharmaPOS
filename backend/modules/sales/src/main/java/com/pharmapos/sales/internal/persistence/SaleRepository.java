package com.pharmapos.sales.internal.persistence;

import com.pharmapos.sales.internal.domain.Sale;
import com.pharmapos.sales.internal.domain.SaleStatus;
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
 * Repository for Sale aggregate root.
 * Provides queries for sales retrieval, filtering, and reporting.
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    /**
     * Find sale by receipt number within tenant.
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.receiptNumber = :receiptNumber
        AND s.tenantId = :tenantId
        """)
    Optional<Sale> findByReceiptNumber(
        @Param("receiptNumber") String receiptNumber,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find sales by shift.
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.shiftId = :shiftId
        AND s.tenantId = :tenantId
        ORDER BY s.completedAt DESC
        """)
    List<Sale> findByShiftId(
        @Param("shiftId") UUID shiftId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find sales by branch and date range.
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.branchId = :branchId
        AND s.completedAt BETWEEN :startDate AND :endDate
        AND s.status = 'COMPLETED'
        AND s.tenantId = :tenantId
        ORDER BY s.completedAt DESC
        """)
    Page<Sale> findByBranchAndDateRange(
        @Param("branchId") UUID branchId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Find sales by customer.
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.customerId = :customerId
        AND s.tenantId = :tenantId
        ORDER BY s.completedAt DESC
        """)
    Page<Sale> findByCustomerId(
        @Param("customerId") UUID customerId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Find sales by status and branch.
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.status = :status
        AND s.branchId = :branchId
        AND s.tenantId = :tenantId
        ORDER BY s.createdAt DESC
        """)
    Page<Sale> findByStatusAndBranch(
        @Param("status") SaleStatus status,
        @Param("branchId") UUID branchId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Calculate total sales amount for shift.
     */
    @Query("""
        SELECT COALESCE(SUM(s.totalAmount), 0)
        FROM Sale s
        WHERE s.shiftId = :shiftId
        AND s.status = 'COMPLETED'
        AND s.tenantId = :tenantId
        """)
    long calculateShiftTotal(
        @Param("shiftId") UUID shiftId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Count sales by shift.
     */
    @Query("""
        SELECT COUNT(s)
        FROM Sale s
        WHERE s.shiftId = :shiftId
        AND s.status = 'COMPLETED'
        AND s.tenantId = :tenantId
        """)
    long countByShift(
        @Param("shiftId") UUID shiftId,
        @Param("tenantId") UUID tenantId
    );
}
