# ADR-002: Hybrid Multi-Tenancy with Row-Level Security

## Status
Accepted

## Context
PharmaPOS serves multiple pharmacy businesses (tenants). Each tenant's data must be strictly isolated for security, compliance, and business reasons.

Options considered:
1. **Separate database per tenant**: Maximum isolation, expensive
2. **Shared database, shared schema with discriminator**: Simple, risk of data leakage
3. **Shared database, separate schema per tenant**: Middle ground
4. **Hybrid: Shared schema with RLS + dedicated DB option**: Flexible

## Decision
Use **hybrid multi-tenancy**:
- **Default**: Pooled schema with PostgreSQL Row-Level Security (RLS)
- **Premium/Regulatory**: Dedicated database for specific tenants
- Same codebase, same migrations, different data sources

### Implementation Details

**Row-Level Security**
- Every table has `tenant_id UUID NOT NULL`
- RLS policies enforce: `tenant_id = current_setting('app.tenant_id')::uuid`
- Transaction-local setting: `SET LOCAL app.tenant_id = :tenantId`
- Application role cannot bypass RLS
- Fail-closed: Missing setting returns zero rows

**Tenant Directory**
```sql
-- Platform database
plt_tenant (
    id UUID,
    slug VARCHAR,
    data_source ENUM('pool', 'silo'),
    db_connection_id UUID,  -- NULL for pool, connection details for silo
    status ENUM('active', 'suspended', 'locked')
)
```

**Request Flow**
1. JWT token contains `tenant_id`
2. Subdomain validates against token tenant
3. TenantContext established (thread-local)
4. Data source router selects pool or silo
5. Transaction sets `app.tenant_id`
6. RLS enforces isolation

## Consequences

### Positive
- **Cost effective**: Most tenants share infrastructure
- **Database-enforced isolation**: RLS is a second barrier even if application code has bugs
- **Premium tier ready**: Large tenants can have dedicated databases
- **Regulatory compliance**: Dedicated DBs for regions requiring data residency
- **Same codebase**: No separate code paths for different tenancy models
- **Easy tenant provisioning**: New tenants start in pool instantly

### Negative
- **Complexity**: RLS policies must be applied to every table (mitigated by migration helpers)
- **Performance overhead**: Small RLS check on every query (negligible in practice)
- **Noisy neighbor risk**: One tenant's heavy queries affect others in pool (mitigated by connection limits)

### Neutral
- **Migration path**: Tenants can move from pool to silo without application changes
- **Testing**: Tests must set tenant context or they see zero rows (good - catches missing tenant filters)
