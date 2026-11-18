# ADR-010 — Stock Exists Only as Batch Balances; Balance Equals Sum of Immutable Movements

## Status

Accepted (Plan §2.3)

## Context

Pharmacy stock management must be batch-aware (each unit of a medicine belongs to a specific batch with a batch number, expiry date, and cost), and must support strict FEFO (First Expiry, First Out) allocation, recalls by batch, quarantine, and expiry management. Additionally, every stock change must be auditable for regulatory compliance.

Two common inventory models exist:

1. **Mutable balance with transaction log:** A balance row is updated directly; a separate log records each change. The balance is the source of truth; the log is supplementary. If they diverge, the balance wins.

2. **Immutable movement ledger:** Every stock change is an immutable `movement` row. The balance is a **derived projection** — it equals the sum of all movements for a batch. If the projection drifts, the movements are recomputed. The movements are the source of truth.

## Decision

Stock exists **only as batch balances**, and a batch balance is defined as the **sum of its immutable movements**. There is no independent "stock level" that can be edited directly.

### Core tables

**`inv_stock_batch`** — One row per batch per branch per tenant. Status: Available, Reserved, Quarantined, Expired, Recalled, Damaged.

**`inv_stock_movement`** — Immutable, append-only, partitioned by month. Each row records one indivisible stock change:

| Movement Type | Description |
|---|---|
| `RECEIPT` | Goods received against a GRN line |
| `SALE` | Stock allocated and sold via FEFO |
| `RETURN` | Returned stock (always into Quarantined) |
| `TRANSFER_OUT` | Stock sent to another branch |
| `TRANSFER_IN` | Stock received from another branch |
| `ADJUSTMENT` | Manual correction (requires reason + approval) |
| `DAMAGE` | Write-off due to damage |
| `LOSS` | Write-off due to loss |
| `EXPIRY_WRITE_OFF` | Write-off due to expiration |
| `QUARANTINE` | Moved to quarantine (recall, inspection) |
| `RELEASE` | Released from quarantine after inspection |

Each movement records `quantity_delta` (signed, in base units using `numeric(14,3)`), the source document (sale line, GRN line, count line), timestamps (server and device), and device origin for edge-sourced movements.

**`inv_stock_balance`** — A projection table with one row per batch: `quantity_on_hand` and `quantity_reserved`. Updated in the **same transaction** as each movement insert. A nightly reconciliation job recomputes all balances from movements and alerts on any drift.

### Integrity rules

1. **Cloud-origin sales:** `SELECT … FOR UPDATE` on the batch balance row; reject if balance would go negative; commit movement + balance update atomically.
2. **Edge-origin sales:** Accepted as facts (they already happened); if the balance goes negative, the cloud raises a negative-stock exception for manager resolution.
3. **No hard `CHECK` constraint** on balance for edge-origin events; the guard is in the application and the exception queue.
4. Returned stock → Quarantined, never directly to Available.

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Mutable balance as source of truth | An unexplained balance change is invisible; a drift between log and balance requires a human decision on which is correct; regulatory audits require an immutable trail |
| Event-sourced aggregate (full CQRS) | Higher implementation complexity; not needed when the movement table is itself the event store for stock; CQRS may be added for reporting read models only |
| SKU-level balance (no batch tracking) | Regulatory non-compliance; cannot do FEFO, recalls, or controlled-substance traceability |

## Consequences

**Positive:**
- Every stock change has a complete, immutable audit trail from day one.
- Balance can always be recomputed from movements — a detected drift is recoverable.
- Recalls are a query: find every movement linked to the recalled batch(es).
- FEFO allocation is a natural query on batch status and expiry date.
- Nightly reconciliation catches software bugs, manual database edits, or any other source of drift automatically.

**Negative / Risks:**
- Insert volume: at 50M movements/year, the `inv_stock_movement` table grows significantly. Mitigation: monthly partitions with old months on cold storage (still queryable).
- Maintaining the balance projection in-transaction adds a write to every stock operation. The balance row is a hot row for high-volume batches. Mitigation: batch splitting keeps per-batch volume manageable; the FEFO algorithm naturally distributes across batches.
- Edge-origin negative balances are accepted (they cannot be prevented without blocking the sale). The exception queue and manager resolution process must be built and monitored from P1.
- `numeric(14,3)` for quantities allows millilitre-level dispensing but costs more storage than `int`; decision to confirm in P2.

## References

- TDD §4.2, §4.3, §7.1–7.2
- Plan §2.3, §2.12, NFR D1
