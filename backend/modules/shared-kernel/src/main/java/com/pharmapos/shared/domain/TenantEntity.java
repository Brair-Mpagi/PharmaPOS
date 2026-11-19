package com.pharmapos.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base entity for multi-tenant data.
 * All tenant-scoped entities inherit from this class.
 * Row-Level Security policies enforce tenant isolation on tenant_id.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
