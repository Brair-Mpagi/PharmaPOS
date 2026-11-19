package com.pharmapos.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base entity for branch-scoped data.
 * Extends TenantEntity with branch_id for branch-specific records.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BranchEntity extends TenantEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;
}
