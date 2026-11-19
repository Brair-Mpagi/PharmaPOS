package com.pharmapos.sales.internal.web;

import com.pharmapos.sales.internal.application.SalesService;
import com.pharmapos.sales.internal.domain.Payment;
import com.pharmapos.sales.internal.domain.Sale;
import com.pharmapos.sales.internal.domain.SaleLine;
import com.pharmapos.sales.internal.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for sales operations.
 * Exposes endpoints for sale lifecycle: draft, add lines, add payments, finalize.
 */
@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    /**
     * POST /api/v1/sales
     * Create a new sale draft.
     */
    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody CreateSaleRequest request) {
        Sale sale = salesService.createSaleDraft(
            request.branchId(),
            request.shiftId(),
            request.customerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SaleResponse.fromDomain(sale));
    }

    /**
     * GET /api/v1/sales/{id}
     * Get sale by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSale(@PathVariable UUID id) {
        return salesService.findById(id)
            .map(SaleResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/sales/receipt/{receiptNumber}
     * Get sale by receipt number.
     */
    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<SaleResponse> getSaleByReceipt(@PathVariable String receiptNumber) {
        return salesService.findByReceiptNumber(receiptNumber)
            .map(SaleResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/sales/{id}/lines
     * Add a line to a sale draft.
     */
    @PostMapping("/{id}/lines")
    public ResponseEntity<SaleLineResponse> addSaleLine(
        @PathVariable UUID id,
        @Valid @RequestBody AddSaleLineRequest request
    ) {
        SaleLine line = salesService.addSaleLine(
            id,
            request.itemId(),
            request.quantity(),
            request.unitPriceMinorUnits()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SaleLineResponse.fromDomain(line));
    }

    /**
     * GET /api/v1/sales/{id}/lines
     * Get all lines for a sale.
     */
    @GetMapping("/{id}/lines")
    public ResponseEntity<List<SaleLineResponse>> getSaleLines(@PathVariable UUID id) {
        List<SaleLine> lines = salesService.getSaleLines(id);
        return ResponseEntity.ok(lines.stream().map(SaleLineResponse::fromDomain).toList());
    }

    /**
     * POST /api/v1/sales/{id}/payments
     * Add a payment to a sale.
     */
    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> addPayment(
        @PathVariable UUID id,
        @Valid @RequestBody AddPaymentRequest request
    ) {
        Payment payment = salesService.addPayment(
            id,
            request.method(),
            request.amountMinorUnits(),
            request.transactionRef()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.fromDomain(payment));
    }

    /**
     * GET /api/v1/sales/{id}/payments
     * Get all payments for a sale.
     */
    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getSalePayments(@PathVariable UUID id) {
        List<Payment> payments = salesService.getSalePayments(id);
        return ResponseEntity.ok(payments.stream().map(PaymentResponse::fromDomain).toList());
    }

    /**
     * POST /api/v1/sales/{id}/finalize
     * Finalize sale: complete transaction atomically.
     * This allocates inventory, validates payment, generates receipt.
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<SaleResponse> finalizeSale(@PathVariable UUID id) {
        Sale sale = salesService.finalizeSale(id);
        return ResponseEntity.ok(SaleResponse.fromDomain(sale));
    }

    /**
     * POST /api/v1/sales/{id}/void
     * Void a sale (cancel before finalization).
     */
    @PostMapping("/{id}/void")
    public ResponseEntity<SaleResponse> voidSale(
        @PathVariable UUID id,
        @RequestParam String reason
    ) {
        Sale sale = salesService.voidSale(id, reason);
        return ResponseEntity.ok(SaleResponse.fromDomain(sale));
    }

    /**
     * GET /api/v1/sales/shift/{shiftId}
     * List all sales for a shift.
     */
    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<List<SaleResponse>> getSalesByShift(@PathVariable UUID shiftId) {
        List<Sale> sales = salesService.findByShift(shiftId);
        return ResponseEntity.ok(sales.stream().map(SaleResponse::fromDomain).toList());
    }

    /**
     * GET /api/v1/sales/branch/{branchId}
     * List sales by branch and date range.
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Page<SaleResponse>> getSalesByBranch(
        @PathVariable UUID branchId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        Pageable pageable
    ) {
        Page<Sale> sales = salesService.findByBranchAndDateRange(
            branchId, startDate, endDate, pageable
        );
        return ResponseEntity.ok(sales.map(SaleResponse::fromDomain));
    }

    /**
     * GET /api/v1/sales/customer/{customerId}
     * List sales by customer (purchase history).
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<SaleResponse>> getSalesByCustomer(
        @PathVariable UUID customerId,
        Pageable pageable
    ) {
        Page<Sale> sales = salesService.findByCustomer(customerId, pageable);
        return ResponseEntity.ok(sales.map(SaleResponse::fromDomain));
    }
}
