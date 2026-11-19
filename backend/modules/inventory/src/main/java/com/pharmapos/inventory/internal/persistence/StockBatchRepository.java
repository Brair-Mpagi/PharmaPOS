package com.pharmapos.inventory.internal.persistence;

import com.pharmapos.inventory.internal.domain.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Stock Batches.
 */
@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, UUID> {

    Optional<StockBatch> findByTenantIdAndBranchIdAndId(UUID tenantId, UUID branchId, UUID id);

    List<StockBatch> findByTenantIdAndBranchIdAndSellableItemId(
        UUID tenantId, 
        UUID branchId, 
        UUID sellableItemId
    );

    /**
     * Find available batches for FEFO allocation.
     * Critical query: sorted by expiry date (earliest first).
     */
    @Query("SELECT b FROM StockBatch b " +
           "WHERE b.tenantId = :tenantId " +
           "AND b.branchId = :branchId " +
           "AND b.sellableItemId = :itemId " +
           "AND b.status = 'AVAILABLE' " +
           "AND (b.expiryDate IS NULL OR b.expiryDate >= :today) " +
           "ORDER BY b.expiryDate ASC NULLS LAST, b.createdAt ASC")
    List<StockBatch> findAvailableBatchesForFEFO(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("itemId") UUID itemId,
        @Param("today") LocalDate today
    );

    /**
     * Find batches expiring soon (for alerts).
     */
    @Query("SELECT b FROM StockBatch b " +
           "WHERE b.tenantId = :tenantId " +
           "AND b.branchId = :branchId " +
           "AND b.status = 'AVAILABLE' " +
           "AND b.expiryDate BETWEEN :today AND :threshold " +
           "ORDER BY b.expiryDate ASC")
    List<StockBatch> findExpiringSoon(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("today") LocalDate today,
        @Param("threshold") LocalDate threshold
    );

    /**
     * Find expired batches that need write-off.
     */
    @Query("SELECT b FROM StockBatch b " +
           "WHERE b.tenantId = :tenantId " +
           "AND b.branchId = :branchId " +
           "AND b.status IN ('AVAILABLE', 'RESERVED') " +
           "AND b.expiryDate < :today")
    List<StockBatch> findExpiredBatches(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("today") LocalDate today
    );

    List<StockBatch> findByTenantIdAndBranchIdAndStatus(
        UUID tenantId,
        UUID branchId,
        StockBatch.BatchStatus status
    );
}
