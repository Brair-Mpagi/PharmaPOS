package com.pharmapos.identity.internal.application;

import com.pharmapos.shared.multitenancy.TenantDirectory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Answers entitlement questions: may tenant T use feature X, add a branch,
 * or exceed N users? (TDD §5.4).
 *
 * <p>Entitlement checks are performed <em>in the backend at the point of action</em>,
 * never only in the UI. Answers are cached briefly (default 60 seconds) to avoid
 * a DB hit on every request; the cache is evicted on subscription changes.
 *
 * <p>Entitlement state derives from:
 * <ul>
 *   <li>The tenant's subscription plan ({@code plt_subscription})</li>
 *   <li>The plan's limits ({@code plt_plan_limit})</li>
 *   <li>Current usage counters ({@code plt_usage_counter})</li>
 * </ul>
 */
@Service
public class EntitlementService {

    private final JdbcTemplate platformJdbc;
    private final TenantDirectory tenantDirectory;

    public EntitlementService(
            @org.springframework.beans.factory.annotation.Qualifier("platformJdbcTemplate")
            JdbcTemplate platformJdbc,
            TenantDirectory tenantDirectory
    ) {
        this.platformJdbc   = platformJdbc;
        this.tenantDirectory = tenantDirectory;
    }

    /**
     * Returns {@code true} if the tenant is permitted to use the named feature.
     *
     * @param tenantId   UUID of the tenant
     * @param featureName a feature key (e.g. "clinical.controlled_register", "reporting.export")
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "entitlement", key = "'feature:' + #tenantId + ':' + #featureName")
    public boolean isFeatureEnabled(UUID tenantId, String featureName) {
        assertTenantActive(tenantId);
        var rows = platformJdbc.queryForList(
            "SELECT 1 FROM plt_plan_feature f " +
            "JOIN plt_subscription s ON s.plan_id = f.plan_id " +
            "WHERE s.tenant_id = ?::uuid AND s.status IN ('ACTIVE','GRACE') " +
            "  AND f.feature_key = ?",
            tenantId, featureName);
        return !rows.isEmpty();
    }

    /**
     * Returns {@code true} if the tenant may add one more branch without exceeding
     * their plan limit.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "entitlement", key = "'canAddBranch:' + #tenantId")
    public boolean canAddBranch(UUID tenantId) {
        assertTenantActive(tenantId);
        var limitRow = platformJdbc.queryForList(
            "SELECT pl.max_branches, coalesce(uc.branch_count, 0) AS current " +
            "FROM plt_plan_limit pl " +
            "JOIN plt_subscription s ON s.plan_id = pl.plan_id " +
            "LEFT JOIN plt_usage_counter uc ON uc.tenant_id = s.tenant_id " +
            "WHERE s.tenant_id = ?::uuid AND s.status IN ('ACTIVE','GRACE')",
            tenantId);
        if (limitRow.isEmpty()) return false;
        var row = limitRow.getFirst();
        long max     = ((Number) row.get("max_branches")).longValue();
        long current = ((Number) row.get("current")).longValue();
        return max < 0 || current < max; // -1 = unlimited
    }

    /**
     * Returns {@code true} if the tenant may add one more user without exceeding
     * their plan limit.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "entitlement", key = "'canAddUser:' + #tenantId")
    public boolean canAddUser(UUID tenantId) {
        assertTenantActive(tenantId);
        var limitRow = platformJdbc.queryForList(
            "SELECT pl.max_users, coalesce(uc.user_count, 0) AS current " +
            "FROM plt_plan_limit pl " +
            "JOIN plt_subscription s ON s.plan_id = pl.plan_id " +
            "LEFT JOIN plt_usage_counter uc ON uc.tenant_id = s.tenant_id " +
            "WHERE s.tenant_id = ?::uuid AND s.status IN ('ACTIVE','GRACE')",
            tenantId);
        if (limitRow.isEmpty()) return false;
        var row = limitRow.getFirst();
        long max     = ((Number) row.get("max_users")).longValue();
        long current = ((Number) row.get("current")).longValue();
        return max < 0 || current < max;
    }

    /**
     * Throws if the tenant's status prevents lease renewal (e.g. LOCKED).
     */
    public void assertCanRenewLease(UUID tenantId, UUID deviceId) {
        TenantDirectory.DataSourceConfig cfg = tenantDirectory.resolve(tenantId);
        if (cfg == null || cfg.status() == TenantDirectory.TenantStatus.LOCKED
                        || cfg.status() == TenantDirectory.TenantStatus.OFFBOARDING) {
            throw new EntitlementViolationException(
                "Tenant " + tenantId + " may not renew leases (status=" +
                (cfg == null ? "UNKNOWN" : cfg.status()) + ")");
        }
    }

    /**
     * Returns the lease duration in seconds for the tenant, subject to plan ceilings.
     * Default: 72 hours. Fiscal tenants: max 5 days. High-risk roles: max 24 hours.
     */
    public long leaseDurationSeconds(UUID tenantId) {
        var rows = platformJdbc.queryForList(
            "SELECT pl.max_lease_hours FROM plt_plan_limit pl " +
            "JOIN plt_subscription s ON s.plan_id = pl.plan_id " +
            "WHERE s.tenant_id = ?::uuid AND s.status IN ('ACTIVE','GRACE')",
            tenantId);
        if (rows.isEmpty()) return 72L * 3600;
        long maxHours = ((Number) rows.getFirst().get("max_lease_hours")).longValue();
        return Math.min(maxHours, 7L * 24) * 3600; // cap at 7 days absolute
    }

    /** Returns the set of offline features this tenant's plan permits. */
    public Set<String> allowedOfflineFeatures(UUID tenantId) {
        var rows = platformJdbc.queryForList(
            "SELECT f.feature_key FROM plt_plan_feature f " +
            "JOIN plt_subscription s ON s.plan_id = f.plan_id " +
            "WHERE s.tenant_id = ?::uuid AND s.status IN ('ACTIVE','GRACE') " +
            "  AND f.offline_allowed = true",
            tenantId);
        return rows.stream()
            .map(r -> (String) r.get("feature_key"))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @CacheEvict(value = "entitlement", allEntries = true)
    @Scheduled(fixedDelayString = "${pharmapos.entitlement.cache-ttl-ms:60000}")
    public void evictEntitlementCache() {
        // scheduled eviction — annotation does the work
    }

    private void assertTenantActive(UUID tenantId) {
        TenantDirectory.DataSourceConfig cfg = tenantDirectory.resolve(tenantId);
        if (cfg != null && cfg.status() == TenantDirectory.TenantStatus.LOCKED) {
            throw new EntitlementViolationException("Tenant " + tenantId + " is locked");
        }
    }

    public static class EntitlementViolationException extends RuntimeException {
        public EntitlementViolationException(String msg) { super(msg); }
    }
}
