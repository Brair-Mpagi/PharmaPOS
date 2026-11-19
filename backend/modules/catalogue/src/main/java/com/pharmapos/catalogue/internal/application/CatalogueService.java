package com.pharmapos.catalogue.internal.application;

import com.pharmapos.catalogue.internal.domain.*;
import com.pharmapos.catalogue.internal.persistence.*;
import com.pharmapos.shared.context.TenantContextHolder;
import com.pharmapos.shared.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Catalogue Service - Core business logic for catalogue management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogueService {

    private final SellableItemRepository itemRepository;
    private final BarcodeRepository barcodeRepository;
    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final TaxClassRepository taxClassRepository;

    // =========================================================================
    // Sellable Items
    // =========================================================================

    public Page<SellableItem> searchItems(String query, Pageable pageable) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }
        
        log.debug("Searching items for tenant {} with query: {}", tenantId, query);
        return itemRepository.searchItems(tenantId, query.trim(), pageable);
    }

    public Optional<SellableItem> getItemById(UUID itemId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return itemRepository.findByTenantIdAndId(tenantId, itemId);
    }

    public Optional<SellableItem> getItemByBarcode(String barcode) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        Optional<Barcode> barcodeOpt = barcodeRepository.findByTenantIdAndCodeAndIsActive(
            tenantId, barcode, true
        );
        
        if (barcodeOpt.isEmpty()) {
            log.debug("Barcode {} not found for tenant {}", barcode, tenantId);
            return Optional.empty();
        }
        
        return itemRepository.findById(barcodeOpt.get().getSellableItemId());
    }

    @Transactional
    public SellableItem createItem(SellableItem item) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        // Validate unique name
        if (itemRepository.existsByTenantIdAndNameIgnoreCase(tenantId, item.getName())) {
            throw new IllegalArgumentException("Item with name '" + item.getName() + "' already exists");
        }
        
        item.setTenantId(tenantId);
        item.setStatus(SellableItem.ItemStatus.ACTIVE);
        
        SellableItem saved = itemRepository.save(item);
        log.info("Created sellable item {} for tenant {}", saved.getId(), tenantId);
        
        return saved;
    }

    @Transactional
    public SellableItem updateItem(UUID itemId, SellableItem updates) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        SellableItem existing = itemRepository.findByTenantIdAndId(tenantId, itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        
        // Update fields
        existing.setName(updates.getName());
        existing.setGenericName(updates.getGenericName());
        existing.setActiveIngredient(updates.getActiveIngredient());
        existing.setStrength(updates.getStrength());
        existing.setDosageForm(updates.getDosageForm());
        existing.setBaseUnit(updates.getBaseUnit());
        existing.setPackSize(updates.getPackSize());
        existing.setManufacturer(updates.getManufacturer());
        existing.setRequiresPrescription(updates.isRequiresPrescription());
        existing.setIsControlled(updates.isControlled());
        existing.setSchedule(updates.getSchedule());
        existing.setDescription(updates.getDescription());
        existing.setStorageConditions(updates.getStorageConditions());
        existing.setTaxClassId(updates.getTaxClassId());
        
        SellableItem saved = itemRepository.save(existing);
        log.info("Updated sellable item {} for tenant {}", itemId, tenantId);
        
        return saved;
    }

    // =========================================================================
    // Barcodes
    // =========================================================================

    @Transactional
    public Barcode addBarcode(UUID itemId, String code, Barcode.BarcodeType type, boolean isPrimary) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        // Verify item exists
        SellableItem item = itemRepository.findByTenantIdAndId(tenantId, itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        
        // Check barcode uniqueness
        if (barcodeRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new IllegalArgumentException("Barcode already exists: " + code);
        }
        
        Barcode barcode = new Barcode();
        barcode.setTenantId(tenantId);
        barcode.setSellableItemId(itemId);
        barcode.setCode(code);
        barcode.setType(type);
        barcode.setIsPrimary(isPrimary);
        barcode.setIsActive(true);
        
        // Validate format
        if (!barcode.isValidFormat()) {
            throw new IllegalArgumentException("Invalid barcode format for type " + type);
        }
        
        Barcode saved = barcodeRepository.save(barcode);
        log.info("Added barcode {} for item {} in tenant {}", code, itemId, tenantId);
        
        return saved;
    }

    public List<Barcode> getItemBarcodes(UUID itemId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return barcodeRepository.findByTenantIdAndSellableItemId(tenantId, itemId);
    }

    // =========================================================================
    // Pricing
    // =========================================================================

    public Optional<Money> getItemPrice(UUID itemId, BigDecimal quantity) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        // Get default active price list
        Optional<PriceList> priceListOpt = priceListRepository
            .findByTenantIdAndIsDefaultAndIsActive(tenantId, true, true);
        
        if (priceListOpt.isEmpty()) {
            log.warn("No default price list found for tenant {}", tenantId);
            return Optional.empty();
        }
        
        PriceList priceList = priceListOpt.get();
        
        // Check validity
        if (!priceList.isCurrentlyValid()) {
            log.warn("Default price list {} is not currently valid", priceList.getId());
            return Optional.empty();
        }
        
        // Find applicable price
        Optional<PriceListItem> priceItemOpt = priceListItemRepository.findApplicablePrice(
            tenantId, priceList.getId(), itemId, quantity
        );
        
        return priceItemOpt.map(PriceListItem::getUnitPrice);
    }

    public Optional<PriceList> getDefaultPriceList() {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return priceListRepository.findByTenantIdAndIsDefaultAndIsActive(tenantId, true, true);
    }

    public List<PriceList> getValidPriceLists() {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return priceListRepository.findValidPriceListsOnDate(tenantId, LocalDate.now());
    }

    @Transactional
    public PriceList createPriceList(PriceList priceList) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        priceList.setTenantId(tenantId);
        priceList.setIsActive(true);
        
        PriceList saved = priceListRepository.save(priceList);
        log.info("Created price list {} for tenant {}", saved.getId(), tenantId);
        
        return saved;
    }

    @Transactional
    public PriceListItem addPriceListItem(UUID priceListId, UUID itemId, Money unitPrice, 
                                          BigDecimal minQuantity, BigDecimal maxQuantity) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        // Verify price list and item exist
        priceListRepository.findByTenantIdAndId(tenantId, priceListId)
            .orElseThrow(() -> new IllegalArgumentException("Price list not found: " + priceListId));
        
        itemRepository.findByTenantIdAndId(tenantId, itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        
        PriceListItem priceListItem = new PriceListItem();
        priceListItem.setTenantId(tenantId);
        priceListItem.setPriceListId(priceListId);
        priceListItem.setSellableItemId(itemId);
        priceListItem.setUnitPrice(unitPrice);
        priceListItem.setMinQuantity(minQuantity);
        priceListItem.setMaxQuantity(maxQuantity);
        
        PriceListItem saved = priceListItemRepository.save(priceListItem);
        log.info("Added price list item for item {} in price list {}", itemId, priceListId);
        
        return saved;
    }

    // =========================================================================
    // Tax Classes
    // =========================================================================

    public List<TaxClass> getActiveTaxClasses() {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return taxClassRepository.findByTenantIdAndIsActive(tenantId, true);
    }

    public Optional<TaxClass> getTaxClassById(UUID taxClassId) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return taxClassRepository.findByTenantIdAndId(tenantId, taxClassId);
    }

    @Transactional
    public TaxClass createTaxClass(TaxClass taxClass) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        
        if (taxClassRepository.existsByTenantIdAndCode(tenantId, taxClass.getCode())) {
            throw new IllegalArgumentException("Tax class with code '" + taxClass.getCode() + "' already exists");
        }
        
        taxClass.setTenantId(tenantId);
        taxClass.setIsActive(true);
        
        TaxClass saved = taxClassRepository.save(taxClass);
        log.info("Created tax class {} for tenant {}", saved.getId(), tenantId);
        
        return saved;
    }
}
