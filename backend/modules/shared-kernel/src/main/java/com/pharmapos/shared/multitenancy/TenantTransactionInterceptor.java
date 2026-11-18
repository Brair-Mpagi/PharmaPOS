package com.pharmapos.shared.multitenancy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AOP aspect that activates PostgreSQL Row-Level Security by setting
 * {@code app.tenant_id} (and {@code app.branch_ids}) at the start of every
 * {@code @Transactional} method (TDD §4.4, §5.1 step 4).
 *
 * <p><b>Critical contract:</b>
 * <ul>
 *   <li>{@code SET LOCAL} is transaction-scoped — the variables are rolled back with
 *       the transaction and never leak to the next caller on the same pooled connection.</li>
 *   <li>A missing {@code app.tenant_id} setting yields {@code NULL} from the RLS policy,
 *       which matches no rows — the system <em>fails closed</em>.</li>
 * </ul>
 *
 * <p><b>Ordering:</b> runs at {@code HIGHEST_PRECEDENCE + 20} — intentionally
 * <em>after</em> {@link com.pharmapos.shared.security.PermissionEnforcementAspect}
 * (order +15) which rejects unauthorised calls before any SQL is issued. This
 * ensures we fail fast on auth rather than opening a transaction and setting RLS
 * variables for a call that will be rejected immediately afterward.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantTransactionInterceptor implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantTransactionInterceptor.class);

    private final JdbcTemplate jdbcTemplate;

    public TenantTransactionInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Around advice for every {@code @Transactional} method.
     * Sets the PostgreSQL session variables before the method body executes
     * so that all repository calls inside the transaction are RLS-filtered.
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) " +
            "|| @within(org.springframework.transaction.annotation.Transactional)")
    public Object aroundTransactional(ProceedingJoinPoint pjp) throws Throwable {
        TenantContext ctx = TenantContextHolder.peek();

        if (ctx == null) {
            // Platform / control-plane code or background job without tenant context.
            // Proceed without setting RLS variables. The DB role (app_rw) has no BYPASSRLS
            // so queries will return empty results, which is the safe default.
            log.trace("No TenantContext at {}; proceeding without RLS vars",
                pjp.getSignature().toShortString());
            return pjp.proceed();
        }

        UUID tenantId = ctx.tenantId();

        // SET LOCAL keeps the setting within the current transaction boundary.
        // UUID.toString() produces only hex chars and hyphens — no SQL injection risk.
        jdbcTemplate.execute("SET LOCAL \"app.tenant_id\" = '" + tenantId + "'");

        if (!ctx.branchGrants().isEmpty()) {
            String csv = ctx.branchGrants().stream()
                .sorted()
                .collect(Collectors.joining(","));
            jdbcTemplate.execute("SET LOCAL \"app.branch_ids\" = '" + csv + "'");
        }

        log.trace("RLS vars set: tenant={} branches={} at {}",
            tenantId, ctx.branchGrants(), pjp.getSignature().toShortString());

        return pjp.proceed();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
