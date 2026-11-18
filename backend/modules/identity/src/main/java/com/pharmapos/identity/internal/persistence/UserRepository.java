package com.pharmapos.identity.internal.persistence;

import com.pharmapos.identity.internal.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} — tenant-scoped by RLS.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<User> findByKeycloakIdAndTenantId(String keycloakId, UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE'")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
}
