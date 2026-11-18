# ADR-005 — Append-Only, Idempotent, Hash-Chained Event Sync

## Status

Accepted (Plan §4.5)

## Context

Branch Local Nodes operate offline for up to 72 hours (configurable to 7 days). Every state change that happens on the BLN during that period — sales, stock movements, dispensing, shift operations — must eventually reach the cloud. The sync protocol must guarantee:

1. **No lost events:** Every local event reaches the cloud, even after a prolonged disconnection.
2. **No duplicated effects:** A retransmitted event must not create a second sale or movement.
3. **Tamper detection:** An altered or deleted event on the BLN must be detectable by the cloud.
4. **Ordering:** Events from a single device are applied in the order they occurred.
5. **Priority:** Cash-related events (shift closes, payments) sync before stock movements.

## Decision

Every state change on the BLN is recorded as an **immutable event** in an append-only journal. Each event carries:

| Field | Purpose |
|---|---|
| `eventId` (UUIDv7) | Globally unique; doubles as the idempotency key |
| `deviceId`, `deviceSeq` | Device identity and a strictly increasing sequence number |
| `tenantId`, `branchId` | Scope (verified against the device certificate by the cloud) |
| `type`, `schemaVersion` | Discriminator and versioning (e.g. `SaleCompleted v1`) |
| `occurredAt`, `deviceClock` | Server-assigned and device-local timestamps |
| `payload` | Business data, validated against the contract schema |
| `prevHash` | Hash of the preceding event in the device's chain |
| `hash` | Hash of this event's content and `prevHash` |
| `signature` | Device key signature over the content and `prevHash` |

**Sync protocol:**

| Step | Endpoint | Behaviour |
|---|---|---|
| Push | `POST /sync/v1/events` | Batched, ordered, over mutual TLS; server stores `eventId` → replay returns original result |
| Validate | (server-side) | Signature, chain continuity, device status, lease validity, tenant/branch scope, business-rule validity |
| Apply | (server-side) | Apply in device order inside transactions; per-event result: accepted, accepted-with-flag, rejected-with-reason |
| Pull | `GET /sync/v1/changes?cursor=…` | Catalogue, prices, users, permissions, batch status, approvals, notifications, configuration since cursor |
| Acknowledge | (response to push) | BLN marks events synced only after cloud acknowledgement; unsynced events are retained |
| Lease | `POST /sync/v1/lease/renew` | Fresh signed lease and key if device and tenant are in good standing |
| Heartbeat | `POST /sync/v1/heartbeat` | Version, pending count, clock skew, integrity state |

After a long disconnection, events push in priority order: shift closes and cash → sales and payments → stock movements → everything else. Sync is throttled and resumable from the last acknowledged `deviceSeq`.

A completed sale is a **fact** and is never silently rejected by the cloud (see TDD §8.5 — conflict handling).

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Last-writer-wins CRDT | Not acceptable for money or stock: a lost sale or double-counted movement is a regulatory violation |
| Message broker (Kafka, RabbitMQ) on the branch | Too much infrastructure for a single-machine branch; the BLN's embedded journal is simpler and self-contained |
| Direct database replication (logical replication to cloud PG) | Requires PostgreSQL on both ends; ties the BLN to PG before ADR-011 is resolved; replication is at the row level, not the domain-event level, so business-rule validation happens after the fact |
| RESTful CRUD sync (no event journal) | No tamper detection; no ordering guarantee; lost-response retries can create duplicates |

## Consequences

**Positive:**
- The hash chain makes tampering detectable: any insertion, deletion, or modification of events breaks the chain.
- Idempotency keys guarantee exactly-once application: a retransmitted event returns the original result.
- Device sequence numbers enforce ordering per device; the cloud applies events in device order.
- The same journal on the BLN feeds local snapshots and disaster recovery; no separate backup mechanism is needed for in-flight events.
- The protocol is explicitly designed for unreliable networks: resumable, throttled, priority-ordered.

**Negative / Risks:**
- Conflicts between offline branches (e.g., the same prescription dispensed at two branches) are not prevented — they are detected and surfaced as exception tasks. The system accepts the sales and raises alerts.
- Negative stock balances can occur after merge; the cloud accepts the fact and flags it.
- The hash chain verification has a performance cost on push; must be tested under load in spike S3 (72-hour soak test).
- Schema evolution of event payloads requires careful versioning (`schemaVersion` field).

## References

- TDD §8.3, §8.4, §8.5
- Plan §4.5, §4.6
