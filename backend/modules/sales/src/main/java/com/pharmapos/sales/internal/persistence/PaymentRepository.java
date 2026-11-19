package com.pharmapos.sales.internal.persistence;

import com.pharmapos.sales.internal.domain.Payment;
import com.pharmapos.sales.internal.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Payment entity.
 * Provides queries for payment tracking and reconciliation.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find all payments for a sale.
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.saleId = :saleId
        AND p.tenantId = :tenantId
        ORDER BY p.createdAt ASC
        """)
    List<Payment> findBySaleId(
        @Param("saleId") UUID saleId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find payments by shift for reconciliation.
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.shiftId = :shiftId
        AND p.tenantId = :tenantId
        ORDER BY p.createdAt ASC
        """)
    List<Payment> findByShiftId(
        @Param("shiftId") UUID shiftId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Calculate total payments by method for a shift.
     * Used in shift reconciliation.
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.shiftId = :shiftId
        AND p.method = :method
        AND p.tenantId = :tenantId
        """)
    long sumByShiftAndMethod(
        @Param("shiftId") UUID shiftId,
        @Param("method") PaymentMethod method,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find payment by transaction reference.
     * Used for MPesa/card payment verification.
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.transactionRef = :transactionRef
        AND p.tenantId = :tenantId
        """)
    List<Payment> findByTransactionRef(
        @Param("transactionRef") String transactionRef,
        @Param("tenantId") UUID tenantId
    );
}
