# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for PharmaPOS.

## ADR Index

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-001](ADR-001-modular-monolith.md) | Modular Monolith Architecture | Accepted |
| [ADR-002](ADR-002-hybrid-tenancy.md) | Hybrid Multi-Tenancy with RLS | Accepted |
| [ADR-003](ADR-003-postgresql.md) | PostgreSQL as System of Record | Accepted |
| [ADR-004](ADR-004-bln-thin-terminals.md) | Branch Local Node with Thin Terminals | Accepted |
| [ADR-005](ADR-005-event-sync.md) | Append-Only Hash-Chained Event Sync | Accepted |
| [ADR-009](ADR-009-money-keys.md) | Money as Integer Minor Units, UUIDv7 Keys | Accepted |
| [ADR-014](ADR-014-flyway.md) | Flyway for Database Migrations | Accepted |

## ADR Template

```markdown
# ADR-XXX: [Title]

## Status
[Proposed | Accepted | Deprecated | Superseded by ADR-XXX]

## Context
What is the issue that we're seeing that is motivating this decision or change?

## Decision
What is the change that we're proposing and/or doing?

## Consequences
What becomes easier or more difficult to do because of this change?

### Positive
- List of benefits

### Negative
- List of drawbacks

### Neutral
- Other implications
```

## How to Create a New ADR

1. Copy the template above
2. Number sequentially (e.g., ADR-018)
3. Write a descriptive title
4. Fill in each section
5. Get team review
6. Update this index
7. Commit to repository
