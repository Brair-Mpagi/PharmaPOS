package com.pharmapos.catalogue.internal.persistence;

import com.pharmapos.catalogue.internal.domain.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Price Lists.
 */
@Repository
public interface PriceListRepository extends JpaRepository<PriceList, UUID> {

    Optional<PriceList> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<PriceList> findByTenantIdAndIsDefaultAndIsActive(UUID tenantId, boolean isDefault, boolean isActive);

    List<PriceList> findByTenantIdAndIsActive(UUID tenantId, boolean isActive);

    @Query("SELECT p FROM PriceList p WHERE p.tenantId = :tenantId " +
           "AND p.isActive = true " +
           "AND p.validFrom <= :date " +
           "AND (p.validTo IS NULL OR p.validTo >= :date)")
    List<PriceList> findValidPriceListsOnDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}
