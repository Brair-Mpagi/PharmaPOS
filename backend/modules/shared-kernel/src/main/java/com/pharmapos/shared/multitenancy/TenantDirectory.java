package com.pharmapos.shared.multitenancy;

import java.util.UUID;

/**
 * Maps tenant IDs to their data-source configurations and lifecycle status.
 *
 * <p>Implementations are expected to cache aggressively (control-plane data
 * changes infrequently) and to refresh asynchronously when notified of a change
 * via the {@code TenantProvisioned} or {@code SubscriptionChanged} events (TDD §5.2).
 *
 * @see InMemoryTenantDirectory
 */
public interface TenantDirectory {

    /**
     * Returns the data-source configuration for the given tenant.
     *
     * @param tenantId the tenant UUID
     * @return the config, or {@code null} if the tenant is unknown
     */
    DataSourceConfig resolve(UUID tenantId);

    /**
     * Forces a refresh of the cached entry for one tenant.
     * Called when a control-plane event indicates a change.
     */
    void refresh(UUID tenantId);

    /**
     * Forces a full reload of all cached entries.
     * Called at startup and on a manual admin trigger.
     */
    void refreshAll();

    /**
     * Configuration and lifecycle status of a tenant's data source.
     *
     * @param tenantId            tenant UUID
     * @param url                 JDBC URL — pool cluster or silo database
     * @param username            database username
     * @param password            database password (fetched from secrets manager at load time)
     * @param silo                true when this tenant has a dedicated silo database
     * @param status              current tenant lifecycle status
     * @param maxPoolSize         HikariCP maximum pool size for this tenant
     * @param minIdle             HikariCP minimum idle connections
     * @param connectionTimeoutMs HikariCP connection timeout in milliseconds
     */
    record DataSourceConfig(
            UUID   tenantId,
            String url,
            String username,
            String password,
            boolean silo,
            TenantStatus status,
            int  maxPoolSize,
            int  minIdle,
            long connectionTimeoutMs
    ) {
        public DataSourceConfig {
            if (tenantId == null)             throw new IllegalArgumentException("tenantId required");
            if (url == null || url.isBlank()) throw new IllegalArgumentException("url required");
            if (maxPoolSize <= 0)        maxPoolSize = 10;
            if (minIdle < 0)             minIdle = 2;
            if (connectionTimeoutMs <= 0) connectionTimeoutMs = 30_000L;
        }
    }

    /**
     * Lifecycle status of a tenant.
     * Drives data-access decisions in {@link TenantAwareDataSource}.
     */
    enum TenantStatus {
        /** Fully operational. */
        ACTIVE,
        /** Subscription lapsed or grace period — read-only data access. */
        READ_ONLY,
        /** Suspended by platform admin — no data access permitted. */
        LOCKED,
        /** Tenant has requested deletion; data export in progress. */
        OFFBOARDING
    }
}
