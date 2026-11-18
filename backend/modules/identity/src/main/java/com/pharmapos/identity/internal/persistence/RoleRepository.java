package com.pharmapos.identity.internal.persistence;

import com.pharmapos.identity.internal.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Role} — tenant-scoped by RLS.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Role> findAllByTenantId(UUID tenantId);

    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);
}
