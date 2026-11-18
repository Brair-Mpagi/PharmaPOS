package com.pharmapos.identity.internal.persistence;

import com.pharmapos.identity.internal.domain.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link DeviceRegistration} — tenant-scoped by RLS.
 */
public interface DeviceRepository extends JpaRepository<DeviceRegistration, UUID> {

    Optional<DeviceRegistration> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeviceRegistration> findAllByTenantIdAndBranchId(UUID tenantId, UUID branchId);

    @Query("SELECT d FROM DeviceRegistration d WHERE d.tenantId = :tenantId " +
           "AND d.status = com.pharmapos.identity.internal.domain.DeviceRegistration.DeviceStatus.ACTIVE " +
           "AND d.branchId IN " +
           "(SELECT a.branchId FROM RoleAssignment a WHERE a.userId = :userId)")
    List<DeviceRegistration> findActiveDevicesByUserId(
        @Param("userId")   UUID userId,
        @Param("tenantId") UUID tenantId
    );
}
