package com.pharmapos.inventory.internal.persistence;

import com.pharmapos.inventory.internal.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Stock Movements.
 * Movements are immutable - no update or delete methods.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByBatchIdOrderByOccurredAtDesc(UUID batchId);

    Page<StockMovement> findByTenantIdAndBranchIdOrderByOccurredAtDesc(
        UUID tenantId,
        UUID branchId,
        Pageable pageable
    );

    @Query("SELECT m FROM StockMovement m " +
           "WHERE m.tenantId = :tenantId " +
           "AND m.branchId = :branchId " +
           "AND m.batchId = :batchId " +
           "AND m.occurredAt BETWEEN :startDate AND :endDate " +
           "ORDER BY m.occurredAt DESC")
    List<StockMovement> findBatchMovementsBetweenDates(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("batchId") UUID batchId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    @Query("SELECT m FROM StockMovement m " +
           "WHERE m.sourceDocType = :docType " +
           "AND m.sourceDocId = :docId " +
           "ORDER BY m.occurredAt ASC")
    List<StockMovement> findBySourceDocument(
        @Param("docType") String docType,
        @Param("docId") UUID docId
    );

    /**
     * Check if movement already exists (idempotency check).
     */
    boolean existsByOriginDeviceIdAndOriginSeq(UUID deviceId, Long sequence);
}
