# Architecture Decision Records

This directory contains all Architecture Decision Records (ADRs) for the PharmaPOS project.
ADRs are numbered sequentially and use the standard template: **Status → Context → Decision → Consequences**.

> [!IMPORTANT]
> ADRs are immutable once accepted. If a decision changes, a new ADR supersedes the old one (which is marked **Superseded**). Do not edit historical decisions in place.

---

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-001](ADR-001-modular-monolith.md) | Modular monolith, not microservices | ✅ Accepted |
| [ADR-002](ADR-002-hybrid-tenancy.md) | Hybrid tenancy: pooled schema with RLS by default, dedicated database on demand | ✅ Accepted |
| [ADR-003](ADR-003-postgresql-system-of-record.md) | PostgreSQL as system of record | ✅ Accepted |
| [ADR-004](ADR-004-bln-thin-terminals.md) | Branch Local Node with thin terminals | ✅ Accepted |
| [ADR-005](ADR-005-event-sync.md) | Append-only, idempotent, hash-chained event sync | ✅ Accepted |
| [ADR-006](ADR-006-nextjs-rendering-strategy.md) | Next.js: terminal is a static export; back-office may use SSR; Spring Boot holds all business rules | ✅ Accepted |
| [ADR-007](ADR-007-keycloak-oidc.md) | Keycloak-class OIDC provider, separate realms for tenants and platform staff | ✅ Accepted |
| [ADR-008](ADR-008-transactional-outbox.md) | Transactional outbox, no message broker initially | ✅ Accepted |
| [ADR-009](ADR-009-money-keys.md) | Money as integer minor units; UTC timestamps; UUIDv7 primary keys | ✅ Accepted |
| [ADR-010](ADR-010-stock-movements.md) | Stock exists only as batch balances; balance equals sum of immutable movements | ✅ Accepted |
| [ADR-011](ADR-011-local-db-engine.md) | Local database engine for the BLN | 🔲 Open (P0 spike) |
| [ADR-012](ADR-012-service-worker-library.md) | Service-worker library (Serwist or Workbox) | 🔲 Open |
| [ADR-013](ADR-013-lan-https-cert.md) | Certificate authority approach for HTTPS on the branch LAN | 🔲 Open (P0 spike, mandatory) |
| [ADR-014](ADR-014-migrations.md) | Flyway or Liquibase for database migrations | 🔲 Open (proposed: Flyway) |
| [ADR-015](ADR-015-hosting-region.md) | Hosting region and data residency | 🔲 Open (Legal) |
| [ADR-016](ADR-016-job-scheduler.md) | Job scheduler library (db-scheduler or JobRunr) | 🔲 Open |
| [ADR-017](ADR-017-stock-valuation.md) | Default stock valuation method (weighted average vs FIFO by batch) | 🔲 Open (Product) |

---

## ADR Template

```markdown
# ADR-NNN — Title

## Status

Accepted | Open | Superseded by ADR-NNN

## Context

What is the situation that calls for a decision? What forces are at play?

## Decision

What is the agreed approach?

## Alternatives Considered

What else was evaluated and why was it not chosen?

## Consequences

What becomes easier or harder as a result? What risks does this introduce?
```

---

## Open Decisions Requiring Action

The following ADRs block downstream work and need resolution in Phase 0:

- **ADR-011** — Must be resolved before BLN development begins (P4). Spike exit criteria: scan-to-line p95 ≤ 300 ms; finalise-sale p95 ≤ 1 s on reference hardware.
- **ADR-013** — Must be resolved before terminal development begins (P3). Camera scanning requires HTTPS on the LAN.
- **ADR-014** — Affects every backend module from P1 onward. The proposal is Flyway.
