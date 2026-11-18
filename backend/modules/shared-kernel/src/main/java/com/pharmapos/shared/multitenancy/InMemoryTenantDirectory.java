package com.pharmapos.shared.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cached {@link TenantDirectory} backed by the platform control-plane
 * database ({@code plt_tenant}).
 *
 * <p>The cache is fully loaded at startup (via {@link ApplicationReadyEvent}) and
 * is refreshed:
 * <ul>
 *   <li>Periodically, every {@code pharmapos.tenant.directory.refresh-interval-ms}
 *       milliseconds (default 5 minutes), via {@link Scheduled}</li>
 *   <li>On demand by calling {@link #refresh(UUID)} from a control-plane event listener</li>
 * </ul>
 *
 * <p>All reads are lock-free ({@link ConcurrentHashMap}). The periodic refresh
 * replaces the map atomically via a clear-then-putAll pattern, which is safe
 * because {@link TenantAwareDataSource#determineTargetDataSource()} can tolerate
 * a brief miss during the swap (it falls through to the default data source).
 */
@Component
public class InMemoryTenantDirectory implements TenantDirectory {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTenantDirectory.class);

    private final JdbcTemplate platformJdbc;
    private final Map<UUID, DataSourceConfig> cache = new ConcurrentHashMap<>();

    /**
     * @param platformJdbc JdbcTemplate pointing at the platform/control-plane database
     *                     (qualifier "platformJdbcTemplate" to avoid ambiguity with
     *                     tenant data sources)
     */
    public InMemoryTenantDirectory(
            @Qualifier("platformJdbcTemplate") JdbcTemplate platformJdbc) {
        this.platformJdbc = platformJdbc;
    }

    @Override
    public DataSourceConfig resolve(UUID tenantId) {
        return cache.get(tenantId);
    }

    @Override
    public void refresh(UUID tenantId) {
        log.debug("Refreshing tenant directory entry for {}", tenantId);
        List<DataSourceConfig> results = platformJdbc.query(
            LOAD_ONE_SQL, (rs, n) -> mapRow(rs), tenantId);
        if (results.isEmpty()) {
            cache.remove(tenantId);
            log.info("Tenant {} removed from directory (not found in control-plane DB)", tenantId);
        } else {
            cache.put(tenantId, results.getFirst());
            log.debug("Tenant {} refreshed: status={}", tenantId, results.getFirst().status());
        }
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${pharmapos.tenant.directory.refresh-interval-ms:300000}")
    public void refreshAll() {
        log.info("Loading full tenant directory from control-plane DB");
        Map<UUID, DataSourceConfig> loaded = new ConcurrentHashMap<>();
        platformJdbc.query(LOAD_ALL_SQL, rs -> {
            DataSourceConfig c = mapRow(rs);
            loaded.put(c.tenantId(), c);
        });
        cache.clear();
        cache.putAll(loaded);
        log.info("Tenant directory ready: {} tenants", cache.size());
    }

    private DataSourceConfig mapRow(ResultSet rs) throws SQLException {
        return new DataSourceConfig(
            UUID.fromString(rs.getString("id")),
            rs.getString("db_url"),
            rs.getString("db_username"),
            rs.getString("db_password"),
            rs.getBoolean("is_silo"),
            TenantStatus.valueOf(rs.getString("status")),
            rs.getInt("max_pool_size"),
            rs.getInt("min_idle"),
            rs.getLong("connection_timeout_ms")
        );
    }

    private static final String LOAD_ALL_SQL = """
            SELECT id, db_url, db_username, db_password, is_silo, status,
                   max_pool_size, min_idle, connection_timeout_ms
            FROM   plt_tenant
            WHERE  status <> 'OFFBOARDING'
                OR offboard_expires_at > now()
            """;

    private static final String LOAD_ONE_SQL = """
            SELECT id, db_url, db_username, db_password, is_silo, status,
                   max_pool_size, min_idle, connection_timeout_ms
            FROM   plt_tenant
            WHERE  id = ?
            """;
}
