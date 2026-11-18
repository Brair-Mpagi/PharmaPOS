# ADR-003 — PostgreSQL as System of Record

## Status

Accepted

## Context

PharmaPOS handles regulated clinical data (prescriptions, dispensing), financial records (sales, payments, supplier invoices, ledger entries), and stock movements that must be accurate and auditable for years. The database is the ultimate source of truth for every transaction.

The system requires:
- ACID transactions for sale finalisation (stock allocation, payment, audit in one atomic commit)
- Row-Level Security for tenant isolation
- JSONB for flexible configuration (regulatory profiles, tenant settings)
- Full-text and trigram search for product and patient lookups
- Partitioning for the high-volume `inv_stock_movement` and `aud_event` tables
- Point-in-time recovery (PITR) for backup and disaster recovery
- Mature managed-service availability from every major cloud provider
- A local-database option for the BLN (addressed separately in ADR-011)

## Decision

Use **PostgreSQL** (version 16, managed service with HA and PITR) as the system of record for all business data. No other OLTP database is introduced.

Specific PostgreSQL features relied upon:

| Feature | Use |
|---|---|
| Row-Level Security | Tenant isolation as a second barrier (ADR-002) |
| `JSONB` | Regulatory profiles, tenant configuration, event payloads |
| `UUID` type + `gen_random_uuid()` / application-generated UUIDv7 | Primary keys |
| `TIMESTAMPTZ` | All timestamps stored in UTC |
| `NUMERIC(14,3)` | Quantities in base units (e.g. ml dispensing) |
| `BIGINT` | Money in minor units |
| `ENUM` types | Status columns (batch status, movement type, payment method) |
| Trigram index (`pg_trgm`) | Fuzzy product name and ingredient search |
| Full-text index (`to_tsvector`) | Full-text product search, same definitions on the BLN |
| Range partitioning | `inv_stock_movement` and `aud_event` by month |
| `SELECT … FOR UPDATE` | Batch balance locking during cloud-origin sale finalisation |
| `set_config` / `current_setting` | Transaction-local tenant context for RLS |
| Logical replication | Future: silo provisioning, read replicas for reports |

A read replica is planned from Phase 1 for reporting queries; the application uses separate `DataSource` beans for the primary and replica.

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| MySQL / MariaDB | No row-level security; weaker JSON support; `ENUM` differences; less mature JSONB-equivalent |
| MongoDB | No ACID multi-document transactions suitable for sale finalisation; weaker relational joins for reporting; no native RLS |
| CockroachDB / distributed SQL | Added operational complexity; distributed transaction overhead; not needed at current scale |
| Separate OLAP store (BigQuery, Redshift) | May be added later for large-scale reporting; not at MVP; PostgreSQL read replicas + summary tables are sufficient |

## Consequences

**Positive:**
- Strong ACID guarantees simplify the sale-finalisation logic (single database transaction).
- RLS support directly enables the pool tenancy model.
- Broad managed-service support (AWS RDS/Aurora, GCP Cloud SQL, Azure Database, Supabase) avoids lock-in.
- The same schema (minus encryption-at-rest differences) runs in the BLN's local embedded database if the spike chooses a PostgreSQL-compatible engine.
- Well-understood backup, PITR, and disaster-recovery tooling.

**Negative / Risks:**
- Vertical scaling has limits; the first horizontal scaling step is read replicas for reports, then silo databases for the largest tenants. Plan: address in Phase 10 when measured.
- Monthly partitions on `inv_stock_movement` require the partition-maintenance job to pre-create future partitions.
- The BLN may use a different engine (see ADR-011); schema parity must be maintained and tested by contract tests.

## References

- TDD §2.1 (stack table), §4.1–4.6
- Plan §5.5, §5.6, NFR D1, NFR S1
