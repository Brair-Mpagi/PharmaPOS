package com.pharmapos.catalogue.internal.persistence;

import com.pharmapos.catalogue.internal.domain.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Price List Items.
 */
@Repository
public interface PriceListItemRepository extends JpaRepository<PriceListItem, UUID> {

    List<PriceListItem> findByPriceListId(UUID priceListId);

    List<PriceListItem> findByTenantIdAndSellableItemId(UUID tenantId, UUID sellableItemId);

    @Query("SELECT p FROM PriceListItem p WHERE p.tenantId = :tenantId " +
           "AND p.priceListId = :priceListId " +
           "AND p.sellableItemId = :sellableItemId " +
           "AND (p.minQuantity IS NULL OR p.minQuantity <= :quantity) " +
           "AND (p.maxQuantity IS NULL OR p.maxQuantity >= :quantity) " +
           "ORDER BY p.minQuantity DESC")
    Optional<PriceListItem> findApplicablePrice(
        @Param("tenantId") UUID tenantId,
        @Param("priceListId") UUID priceListId,
        @Param("sellableItemId") UUID sellableItemId,
        @Param("quantity") BigDecimal quantity
    );
}
