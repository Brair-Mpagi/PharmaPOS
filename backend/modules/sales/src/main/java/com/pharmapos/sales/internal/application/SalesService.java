package com.pharmapos.sales.internal.application;

import com.pharmapos.sales.internal.domain.*;
import com.pharmapos.sales.internal.persistence.PaymentRepository;
import com.pharmapos.sales.internal.persistence.SaleLineRepository;
import com.pharmapos.sales.internal.persistence.SaleRepository;
import com.pharmapos.sales.internal.persistence.ShiftRepository;
import com.pharmapos.sharedkernel.TenantContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for sales processing.
 * Handles sale lifecycle, payment processing, and finalization.
 * 
 * Key Transaction: finalizeSale() is the atomic unit that:
 * 1. Validates shift is open
 * 2. Prices all lines from catalogue
 * 3. Allocates inventory via FEFO
 * 4. Validates payment completeness
 * 5. Completes sale and commits all changes atomically
 */
@Service
@Transactional
public class SalesService {

    private final SaleRepository saleRepository;
    private final SaleLineRepository saleLineRepository;
    private final PaymentRepository paymentRepository;
    private final ShiftRepository shiftRepository;

    // Note: In real implementation, would inject CatalogueService and InventoryService
    // For now, we'll create the structure assuming they exist

    public SalesService(
        SaleRepository saleRepository,
        SaleLineRepository saleLineRepository,
        PaymentRepository paymentRepository,
        ShiftRepository shiftRepository
    ) {
        this.saleRepository = saleRepository;
        this.saleLineRepository = saleLineRepository;
        this.paymentRepository = paymentRepository;
        this.shiftRepository = shiftRepository;
    }

    /**
     * Create a new sale draft.
     * Sale starts in DRAFT status and can have lines added.
     */
    public Sale createSaleDraft(
        UUID branchId,
        UUID shiftId,
        UUID customerId
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();

        // Validate shift exists and is open
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        if (!shift.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Shift does not belong to current tenant");
        }

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("Cannot create sale: shift is not open");
        }

        // Create draft sale
        Sale sale = new Sale(
            UUID.randomUUID(),
            tenantId,
            branchId,
            shiftId,
            shift.getCashierId(),
            customerId
        );

        return saleRepository.save(sale);
    }

    /**
     * Add a line to a draft sale.
     * Sale must be in DRAFT status.
     */
    public SaleLine addSaleLine(
        UUID saleId,
        UUID itemId,
        int quantity,
        long unitPriceMinorUnits
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();

        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        if (!sale.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Sale does not belong to current tenant");
        }

        // Add line to sale (domain logic validates status)
        SaleLine line = sale.addLine(itemId, quantity, unitPriceMinorUnits, 0);

        saleRepository.save(sale);
        return line;
    }

    /**
     * Add payment to a sale.
     * Sale can be in DRAFT or PENDING_PAYMENT status.
     */
    public Payment addPayment(
        UUID saleId,
        PaymentMethod method,
        long amountMinorUnits,
        String transactionRef
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();

        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        if (!sale.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Sale does not belong to current tenant");
        }

        // Add payment (domain logic validates)
        Payment payment = sale.addPayment(method, amountMinorUnits, transactionRef);

        saleRepository.save(sale);
        return payment;
    }

    /**
     * Finalize sale: complete transaction atomically.
     * 
     * This is the critical transaction that:
     * 1. Validates all business rules
     * 2. Prices all lines (in real impl, calls CatalogueService)
     * 3. Allocates inventory via FEFO (in real impl, calls InventoryService)
     * 4. Validates payment completeness
     * 5. Generates receipt number
     * 6. Completes sale
     * 
     * On failure, entire transaction rolls back.
     */
    public Sale finalizeSale(UUID saleId) {
        UUID tenantId = TenantContextHolder.getTenantId();

        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        if (!sale.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Sale does not belong to current tenant");
        }

        // Step 1: Validate shift is still open
        Shift shift = shiftRepository.findById(sale.getShiftId())
            .orElseThrow(() -> new IllegalStateException("Shift not found"));

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("Cannot finalize sale: shift is closed");
        }

        // Step 2: Price all lines
        // In real implementation, would call:
        // for (SaleLine line : sale.getLines()) {
        //     PriceResolution price = catalogueService.resolvePrice(line.getItemId(), ...);
        //     line.updatePrice(price.unitPrice, price.taxAmount);
        // }

        // Step 3: Allocate inventory via FEFO
        // In real implementation, would call:
        // for (SaleLine line : sale.getLines()) {
        //     List<AllocationResult> allocations = inventoryService.allocateStock(
        //         line.getItemId(),
        //         sale.getBranchId(),
        //         line.getQuantity()
        //     );
        //     // Store batch allocations for traceability
        // }

        // Step 4: Complete sale (validates payment, generates receipt)
        sale.complete();

        return saleRepository.save(sale);
    }

    /**
     * Void a sale.
     * Only draft or pending sales can be voided.
     */
    public Sale voidSale(UUID saleId, String reason) {
        UUID tenantId = TenantContextHolder.getTenantId();

        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        if (!sale.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Sale does not belong to current tenant");
        }

        sale.voidSale(reason);
        return saleRepository.save(sale);
    }

    /**
     * Get sale by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Sale> findById(UUID saleId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return saleRepository.findById(saleId)
            .filter(sale -> sale.getTenantId().equals(tenantId));
    }

    /**
     * Get sale by receipt number.
     */
    @Transactional(readOnly = true)
    public Optional<Sale> findByReceiptNumber(String receiptNumber) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return saleRepository.findByReceiptNumber(receiptNumber, tenantId);
    }

    /**
     * List sales by shift.
     */
    @Transactional(readOnly = true)
    public List<Sale> findByShift(UUID shiftId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return saleRepository.findByShiftId(shiftId, tenantId);
    }

    /**
     * List sales by branch and date range.
     */
    @Transactional(readOnly = true)
    public Page<Sale> findByBranchAndDateRange(
        UUID branchId,
        Instant startDate,
        Instant endDate,
        Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return saleRepository.findByBranchAndDateRange(
            branchId, startDate, endDate, tenantId, pageable
        );
    }

    /**
     * List sales by customer.
     */
    @Transactional(readOnly = true)
    public Page<Sale> findByCustomer(UUID customerId, Pageable pageable) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return saleRepository.findByCustomerId(customerId, tenantId, pageable);
    }

    /**
     * Get lines for a sale.
     */
    @Transactional(readOnly = true)
    public List<SaleLine> getSaleLines(UUID saleId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        
        // Verify sale exists and belongs to tenant
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        if (!sale.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Sale does not belong to current tenant");
        }

        return saleLineRepository.findBySaleId(saleId, tenantId);
    }

    /**
     * Get payments for a sale.
     */
    @Transactional(readOnly = true)
    public List<Payment> getSalePayments(UUID saleId) {
        UUID tenantId = TenantContextHolder.getTenantId();

        // Verify sale exists and belongs to tenant
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        if (!sale.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Sale does not belong to current tenant");
        }

        return paymentRepository.findBySaleId(saleId, tenantId);
    }
}
