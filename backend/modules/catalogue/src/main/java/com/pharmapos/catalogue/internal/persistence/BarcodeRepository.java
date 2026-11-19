package com.pharmapos.catalogue.internal.persistence;

import com.pharmapos.catalogue.internal.domain.Barcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Barcodes.
 */
@Repository
public interface BarcodeRepository extends JpaRepository<Barcode, UUID> {

    Optional<Barcode> findByTenantIdAndCodeAndIsActive(UUID tenantId, String code, boolean isActive);

    List<Barcode> findBySellableItemIdAndIsActive(UUID sellableItemId, boolean isActive);

    List<Barcode> findByTenantIdAndSellableItemId(UUID tenantId, UUID sellableItemId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    Optional<Barcode> findByTenantIdAndSellableItemIdAndIsPrimary(
        UUID tenantId, 
        UUID sellableItemId, 
        boolean isPrimary
    );
}
