package com.pharmapos.catalogue.internal.persistence;

import com.pharmapos.catalogue.internal.domain.TaxClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Tax Classes.
 */
@Repository
public interface TaxClassRepository extends JpaRepository<TaxClass, UUID> {

    Optional<TaxClass> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<TaxClass> findByTenantIdAndCode(UUID tenantId, String code);

    List<TaxClass> findByTenantIdAndIsActive(UUID tenantId, boolean isActive);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
