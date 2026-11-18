package com.pharmapos.shared.multitenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routing {@link DataSource} that selects the connection pool for the current
 * tenant on every {@code getConnection()} call (TDD §5.2).
 *
 * <p>Pool tenants share the default data source (the shared cluster). Silo
 * tenants each get a dedicated {@link HikariDataSource} that is created lazily
 * on first access and cached for the lifetime of the application.
 *
 * <p>Application code never sees which pool it got — routing is entirely
 * transparent to repositories and use-case services.
 */
@Component
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    private final TenantDirectory tenantDirectory;
    private final ConcurrentHashMap<String, DataSource> siloSources = new ConcurrentHashMap<>();

    /**
     * @param tenantDirectory   directory that maps tenant IDs to data-source configs
     * @param defaultDataSource the shared pool data source used by pool tenants
     */
    public TenantAwareDataSource(TenantDirectory tenantDirectory, DataSource defaultDataSource) {
        this.tenantDirectory = tenantDirectory;
        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(Map.of()); // start empty; we resolve lazily below
        afterPropertiesSet();
    }

    /**
     * Returns the lookup key for the current tenant, or {@code null} when
     * there is no tenant context (health checks, platform endpoints) which
     * causes Spring to fall through to the default data source.
     */
    @Override
    protected Object determineCurrentLookupKey() {
        TenantContext ctx = TenantContextHolder.peek();
        return ctx == null ? null : ctx.tenantId().toString();
    }

    /**
     * Overrides the default routing to implement lazy silo pool creation
     * and tenant status checks.
     */
    @Override
    protected DataSource determineTargetDataSource() {
        Object key = determineCurrentLookupKey();
        if (key == null) {
            return (DataSource) getResolvedDefaultDataSource();
        }

        UUID tenantId = UUID.fromString(key.toString());
        TenantDirectory.DataSourceConfig cfg = tenantDirectory.resolve(tenantId);
        if (cfg == null) {
            throw new TenantNotFoundException("No data-source config for tenant: " + tenantId);
        }

        return switch (cfg.status()) {
            case LOCKED      -> throw new TenantLockedException(
                                    "Tenant " + tenantId + " is locked — no data access permitted");
            case OFFBOARDING -> throw new TenantOffboardingException(
                                    "Tenant " + tenantId + " is being offboarded");
            case ACTIVE, READ_ONLY -> {
                if (!cfg.silo()) {
                    // Pool tenant → shared cluster
                    yield (DataSource) getResolvedDefaultDataSource();
                }
                // Silo tenant → lazily-created dedicated pool
                yield siloSources.computeIfAbsent(key.toString(), k -> {
                    log.info("Creating silo connection pool for tenant {}", tenantId);
                    return buildSiloPool(cfg);
                });
            }
        };
    }

    private HikariDataSource buildSiloPool(TenantDirectory.DataSourceConfig cfg) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(cfg.url());
        hc.setUsername(cfg.username());
        hc.setPassword(cfg.password());
        hc.setMaximumPoolSize(cfg.maxPoolSize());
        hc.setMinimumIdle(cfg.minIdle());
        hc.setConnectionTimeout(cfg.connectionTimeoutMs());
        hc.setPoolName("HikariPool-silo-" + cfg.tenantId());
        hc.addDataSourceProperty("ApplicationName", "pharmapos");
        return new HikariDataSource(hc);
    }

    /**
     * Evicts and closes a silo pool — call during offboarding or pool-to-silo
     * migration cut-over.
     *
     * @param tenantId the tenant whose silo pool should be evicted
     */
    public void evictSilo(UUID tenantId) {
        DataSource ds = siloSources.remove(tenantId.toString());
        if (ds instanceof HikariDataSource hds) {
            log.info("Closing silo pool for tenant {}", tenantId);
            hds.close();
        }
    }

    // ── Typed exceptions ────────────────────────────────────────────────────

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String msg) { super(msg); }
    }

    public static class TenantLockedException extends RuntimeException {
        public TenantLockedException(String msg) { super(msg); }
    }

    public static class TenantOffboardingException extends RuntimeException {
        public TenantOffboardingException(String msg) { super(msg); }
    }
}
