# ADR-002 — Hybrid Tenancy: Pooled Schema with RLS by Default, Dedicated Database on Demand

## Status

Accepted (Plan §5.1)

## Context

PharmaPOS is a multi-tenant SaaS platform targeting up to 1,000 tenants at launch and 3,000 branches platform-wide. Tenants range from single-branch independent pharmacies to multi-branch retail chains with strict data-isolation requirements.

Two common tenancy models exist:

1. **Pool (shared schema, shared tables, row-level isolation):** All tenants share tables; a `tenant_id` column and Row-Level Security policies enforce isolation at the database layer. Low cost, operationally simple, easy to query across tenants for platform analytics.

2. **Silo (dedicated database per tenant):** Each tenant gets its own PostgreSQL database or even its own cluster. Maximum isolation, simplest data export, easiest compliance for high-sensitivity customers. High operational cost: N migration runs, N connection pools, N backup jobs.

Neither model alone suits the full range of customers.

## Decision

Default all new tenants to the **pool database** (`pharmapos_pool`) with PostgreSQL Row-Level Security enforcing tenant isolation at the database layer, as a second independent barrier in addition to application-layer permission checks.

For enterprise customers who require database-level isolation (compliance, contractual, data-residency reasons), provision a **silo database**. The application never knows which model it is on: a routing `DataSource` looks up the tenant in a short-lived cache and returns the appropriate connection pool.

### RLS policy pattern (applied to every table with `tenant_id`)

```sql
ALTER TABLE sal_sale ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_sale FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON sal_sale
  USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
  WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
```

The application sets the tenant at the start of every transaction:
```sql
SELECT set_config('app.tenant_id', :tenantId, true); -- transaction-local
```

A missing setting yields NULL → matches no rows → **fail closed**.

### Database roles

| Role | Privileges | Used by |
|---|---|---|
| `app_rw` | DML on tenant tables; no DDL; no BYPASSRLS | Application at runtime |
| `app_ro_reports` | SELECT on tenant tables via read replica | Reporting jobs |
| `migrator` | DDL owner; not used at runtime | Pipeline only |
| `platform_admin` | Control-plane tables only; BYPASSRLS on `plt_` tables | Platform console |

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Pure silo (all tenants get dedicated DBs) | Operational cost is prohibitive at launch; 1,000 migration pipelines, 1,000 backup schedules |
| Pure pool (no silo option) | Blocks enterprise sales requiring isolation; compliance risk for large pharmacy chains |
| Schema-per-tenant (one schema per tenant in one database) | PostgreSQL RLS is simpler and better tested; schema-per-tenant makes migrations more complex without clear benefit |

## Consequences

**Positive:**
- Commodity tenants (the majority) are cheap to operate and easy to migrate.
- The pool-to-silo move is a planned, tested operation (export, verify, cut-over); the same codebase serves both.
- RLS is a second independent barrier: even a bug in application-layer tenant resolution cannot leak another tenant's rows.
- Platform analytics across tenants run in the pool database under the `platform_admin` role.

**Negative / Risks:**
- Two migration pipelines (pool + each silo) must stay in sync; the CI/CD pipeline loops over all databases.
- A noisy-neighbour tenant on the pool can affect connection counts; mitigation: per-tenant connection caps enforced by PgBouncer or the routing data source.
- Pool-to-silo migration must be rehearsed in Phase 10 before it is offered to customers.
- Spike S4 must confirm that RLS overhead under realistic pool load is within acceptable latency budgets.

## References

- TDD §2.2, §4.4, §5.1–5.2
- Plan §5.1, §5.4
