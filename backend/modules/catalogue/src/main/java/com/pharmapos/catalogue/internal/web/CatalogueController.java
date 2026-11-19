package com.pharmapos.catalogue.internal.web;

import com.pharmapos.catalogue.internal.application.CatalogueService;
import com.pharmapos.catalogue.internal.domain.Barcode;
import com.pharmapos.catalogue.internal.domain.PriceList;
import com.pharmapos.catalogue.internal.domain.SellableItem;
import com.pharmapos.catalogue.internal.domain.TaxClass;
import com.pharmapos.catalogue.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Catalogue operations.
 */
@RestController
@RequestMapping("/api/v1/catalogue")
@RequiredArgsConstructor
@Slf4j
public class CatalogueController {

    private final CatalogueService catalogueService;

    // =========================================================================
    // Sellable Items
    // =========================================================================

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<Page<SellableItemResponse>> searchItems(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        
        log.debug("Searching items with query: {}", query);
        Page<SellableItem> items = catalogueService.searchItems(query, pageable);
        Page<SellableItemResponse> response = items.map(SellableItemResponse::from);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<SellableItemResponse> getItemById(@PathVariable UUID id) {
        return catalogueService.getItemById(id)
                .map(SellableItemResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/items/by-barcode/{barcode}")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<SellableItemResponse> getItemByBarcode(@PathVariable String barcode) {
        return catalogueService.getItemByBarcode(barcode)
                .map(SellableItemResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('catalogue.write')")
    public ResponseEntity<SellableItemResponse> createItem(
            @Valid @RequestBody SellableItemRequest request) {
        
        SellableItem item = mapToEntity(request);
        SellableItem created = catalogueService.createItem(item);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SellableItemResponse.from(created));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('catalogue.write')")
    public ResponseEntity<SellableItemResponse> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody SellableItemRequest request) {
        
        SellableItem updates = mapToEntity(request);
        SellableItem updated = catalogueService.updateItem(id, updates);
        
        return ResponseEntity.ok(SellableItemResponse.from(updated));
    }

    // =========================================================================
    // Barcodes
    // =========================================================================

    @GetMapping("/items/{itemId}/barcodes")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<List<Barcode>> getItemBarcodes(@PathVariable UUID itemId) {
        List<Barcode> barcodes = catalogueService.getItemBarcodes(itemId);
        return ResponseEntity.ok(barcodes);
    }

    @PostMapping("/items/{itemId}/barcodes")
    @PreAuthorize("hasAuthority('catalogue.write')")
    public ResponseEntity<Barcode> addBarcode(
            @PathVariable UUID itemId,
            @Valid @RequestBody BarcodeRequest request) {
        
        Barcode barcode = catalogueService.addBarcode(
                itemId,
                request.getCode(),
                request.getType(),
                request.getIsPrimary()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(barcode);
    }

    // =========================================================================
    // Price Lists
    // =========================================================================

    @GetMapping("/price-lists")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<List<PriceList>> getValidPriceLists() {
        List<PriceList> priceLists = catalogueService.getValidPriceLists();
        return ResponseEntity.ok(priceLists);
    }

    @GetMapping("/price-lists/default")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<PriceList> getDefaultPriceList() {
        return catalogueService.getDefaultPriceList()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/price-lists")
    @PreAuthorize("hasAuthority('catalogue.write')")
    public ResponseEntity<PriceList> createPriceList(
            @Valid @RequestBody PriceListRequest request) {
        
        PriceList priceList = mapToEntity(request);
        PriceList created = catalogueService.createPriceList(priceList);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // Tax Classes
    // =========================================================================

    @GetMapping("/tax-classes")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<List<TaxClass>> getActiveTaxClasses() {
        List<TaxClass> taxClasses = catalogueService.getActiveTaxClasses();
        return ResponseEntity.ok(taxClasses);
    }

    @GetMapping("/tax-classes/{id}")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<TaxClass> getTaxClass(@PathVariable UUID id) {
        return catalogueService.getTaxClassById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private SellableItem mapToEntity(SellableItemRequest request) {
        SellableItem item = new SellableItem();
        item.setName(request.getName());
        item.setGenericName(request.getGenericName());
        item.setActiveIngredient(request.getActiveIngredient());
        item.setStrength(request.getStrength());
        item.setDosageForm(request.getDosageForm());
        item.setBaseUnit(request.getBaseUnit());
        item.setPackSize(request.getPackSize());
        item.setManufacturer(request.getManufacturer());
        item.setRequiresPrescription(request.getRequiresPrescription());
        item.setIsControlled(request.getIsControlled());
        item.setSchedule(request.getSchedule());
        item.setDescription(request.getDescription());
        item.setStorageConditions(request.getStorageConditions());
        item.setTaxClassId(request.getTaxClassId());
        return item;
    }

    private PriceList mapToEntity(PriceListRequest request) {
        PriceList priceList = new PriceList();
        priceList.setName(request.getName());
        priceList.setDescription(request.getDescription());
        priceList.setIsDefault(request.getIsDefault());
        priceList.setValidFrom(request.getValidFrom());
        priceList.setValidTo(request.getValidTo());
        priceList.setCurrency(request.getCurrency());
        return priceList;
    }
}
