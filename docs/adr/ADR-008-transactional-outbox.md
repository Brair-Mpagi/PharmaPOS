# ADR-008 — Transactional Outbox, No Message Broker Initially

## Status

Accepted

## Context

The modular monolith (ADR-001) requires asynchronous communication between modules for events that do not need to be part of the synchronous sale transaction. For example:

- `SaleCompleted` → finance posts the journal entry, customer balance updates, report read models refresh.
- `BatchStatusChanged` (recall, quarantine) → sync gateway propagates to branches; notifications sent.
- `GoodsReceived` → finance posts the accrual and supplier ledger entry.

The classic approach is a message broker (Kafka, RabbitMQ). However, introducing a broker adds:
- An additional infrastructure component to operate, monitor, and secure.
- Dual-write risk: writing to both the database and the broker without a two-phase commit can lose or duplicate messages.
- Operational complexity disproportionate to the team size at launch.

## Decision

Use a **transactional outbox pattern** with no external message broker:

1. The outbox row is written **in the same database transaction** as the state change. No dual-write risk.
2. A relay thread reads unsent rows, dispatches them to in-process event handlers, and marks them sent.
3. Handlers are idempotent: processing the same event twice produces the same result (using the event's `eventId` as the idempotency key).
4. Failed events are retried with exponential back-off. Poison messages (events that fail repeatedly) are moved to a **dead-letter table** with an alert.
5. Event ordering is guaranteed per-entity (using the entity's ID and a sequence column in the outbox table).

### Outbox table (proposed)

```sql
CREATE TABLE outbox (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    aggregate_type TEXT NOT NULL,       -- e.g. 'Sale', 'Batch'
    aggregate_id  UUID NOT NULL,
    event_type    TEXT NOT NULL,        -- e.g. 'SaleCompleted'
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at       TIMESTAMPTZ,
    retry_count   INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ
);
```

### When to revisit

Introduce a message broker when:
- The relay's polling latency is unacceptable for a use case (measured, not assumed).
- Cross-service communication is needed after a module extraction (the sync gateway is the first extraction candidate).
- Event volume exceeds what the outbox polling can handle (50M+ events/year projected; well within PostgreSQL capacity at launch).

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Kafka from the start | Operational overhead disproportionate to the team and load; dual-write risk without an outbox anyway; Kafka is the right tool for a distributed system, not for in-process module communication |
| RabbitMQ from the start | Same dual-write risk; adds an infrastructure component that the team must operate; in-process dispatch is simpler for a monolith |
| Spring ApplicationEvent only (no outbox) | Events are lost if the process crashes after commit but before the event is handled; no retry, no dead-letter, no durable delivery |
| CDC (Change Data Capture) with Debezium | Strong option for outbox relay at scale; adds a Kafka Connect dependency; evaluate when the relay's polling overhead is measured as a bottleneck |

## Consequences

**Positive:**
- Zero additional infrastructure at launch; the outbox is just a PostgreSQL table.
- No dual-write risk: the state change and the event are in one transaction.
- Consumers are idempotent by design, which is required anyway for the BLN sync protocol (ADR-005).
- The relay is a simple, testable component.
- Dead-letter table with alerts prevents silent data loss.

**Negative / Risks:**
- Polling the outbox table adds a small latency (configurable polling interval; typically 100–500 ms).
- Under high event volume, the relay becomes a hot loop on the outbox table. Mitigation: PostgreSQL `LISTEN/NOTIFY` can replace polling; or Debezium CDC.
- No fan-out to external consumers (e.g., third-party webhooks) until a broker is introduced. Mitigation: the public API in Phase 10 will add webhook delivery as an application-layer feature.

## References

- TDD §3.4
- Plan §5.5
