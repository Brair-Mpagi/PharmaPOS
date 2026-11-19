package com.pharmapos.inventory.internal.persistence;

import com.pharmapos.inventory.internal.domain.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Stock Balances.
 */
@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, UUID> {

    Optional<StockBalance> findByTenantIdAndBranchIdAndBatchId(
        UUID tenantId,
        UUID branchId,
        UUID batchId
    );

    /**
     * Lock balance for update (prevents overselling).
     * Used in sale transactions from cloud.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBalance b " +
           "WHERE b.tenantId = :tenantId " +
           "AND b.branchId = :branchId " +
           "AND b.batchId = :batchId")
    Optional<StockBalance> findByBatchIdForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("batchId") UUID batchId
    );

    List<StockBalance> findByTenantIdAndBranchIdAndBatchIdIn(
        UUID tenantId,
        UUID branchId,
        List<UUID> batchIds
    );

    /**
     * Find all balances for batches of a specific item.
     */
    @Query("SELECT bal FROM StockBalance bal " +
           "JOIN StockBatch bat ON bat.id = bal.batchId " +
           "WHERE bal.tenantId = :tenantId " +
           "AND bal.branchId = :branchId " +
           "AND bat.sellableItemId = :itemId")
    List<StockBalance> findBalancesForItem(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("itemId") UUID itemId
    );
}
