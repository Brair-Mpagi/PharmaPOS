package com.pharmapos.sales.internal.persistence;

import com.pharmapos.sales.internal.domain.SaleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SaleLine entity.
 * Provides queries for sales analysis and reporting.
 */
@Repository
public interface SaleLineRepository extends JpaRepository<SaleLine, UUID> {

    /**
     * Find all lines for a sale.
     */
    @Query("""
        SELECT sl FROM SaleLine sl
        WHERE sl.saleId = :saleId
        AND sl.tenantId = :tenantId
        ORDER BY sl.lineNumber ASC
        """)
    List<SaleLine> findBySaleId(
        @Param("saleId") UUID saleId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find top-selling items by quantity.
     * Used for sales analytics and inventory planning.
     */
    @Query("""
        SELECT sl.itemId, SUM(sl.quantity) as totalQty
        FROM SaleLine sl
        JOIN Sale s ON s.id = sl.saleId
        WHERE s.branchId = :branchId
        AND s.completedAt BETWEEN :startDate AND :endDate
        AND s.status = 'COMPLETED'
        AND sl.tenantId = :tenantId
        GROUP BY sl.itemId
        ORDER BY totalQty DESC
        """)
    List<Object[]> findTopSellingItems(
        @Param("branchId") UUID branchId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Calculate total revenue by item.
     */
    @Query("""
        SELECT sl.itemId, SUM(sl.lineTotal) as revenue
        FROM SaleLine sl
        JOIN Sale s ON s.id = sl.saleId
        WHERE s.branchId = :branchId
        AND s.completedAt BETWEEN :startDate AND :endDate
        AND s.status = 'COMPLETED'
        AND sl.tenantId = :tenantId
        GROUP BY sl.itemId
        ORDER BY revenue DESC
        """)
    List<Object[]> calculateRevenueByItem(
        @Param("branchId") UUID branchId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("tenantId") UUID tenantId
    );
}
