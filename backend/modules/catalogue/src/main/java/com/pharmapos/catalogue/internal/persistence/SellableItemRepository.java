package com.pharmapos.catalogue.internal.persistence;

import com.pharmapos.catalogue.internal.domain.SellableItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Sellable Items.
 */
@Repository
public interface SellableItemRepository extends JpaRepository<SellableItem, UUID> {

    List<SellableItem> findByTenantIdAndStatus(UUID tenantId, SellableItem.ItemStatus status);

    Optional<SellableItem> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT s FROM SellableItem s WHERE s.tenantId = :tenantId " +
           "AND s.status = 'ACTIVE' " +
           "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.genericName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.activeIngredient) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SellableItem> searchItems(@Param("tenantId") UUID tenantId, 
                                    @Param("query") String query, 
                                    Pageable pageable);

    @Query("SELECT s FROM SellableItem s WHERE s.tenantId = :tenantId " +
           "AND s.status = 'ACTIVE' " +
           "AND s.requiresPrescription = :requiresPrescription")
    List<SellableItem> findByTenantIdAndRequiresPrescription(
        @Param("tenantId") UUID tenantId,
        @Param("requiresPrescription") boolean requiresPrescription
    );

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
