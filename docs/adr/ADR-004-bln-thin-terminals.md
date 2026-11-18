# ADR-004 — Branch Local Node with Thin Terminals

## Status

Accepted (Plan §4.1)

## Context

Pharmacy branches in the target market experience frequent and prolonged internet outages. Sales, dispensing, and stock management must continue uninterrupted regardless of connectivity. Two architecture options exist:

1. **Per-browser offline-first PWA:** Each terminal (browser) caches its own copy of the product catalogue, prices, and pending sales in IndexedDB. Sync happens directly between each browser and the cloud.

2. **Branch Local Node (BLN) with thin terminals:** A single on-premise server holds an authoritative copy of the branch's data in an encrypted local database. Terminals are lightweight clients that talk to the BLN over the LAN. The BLN syncs with the cloud.

## Decision

Deploy a **Branch Local Node** per branch, with terminals as thin static-export PWAs served by the BLN over the LAN. The BLN is the single source of truth at the branch level while offline; the cloud is the ultimate system of record.

The BLN is the same Spring Boot codebase as Cloud Core, running with an **edge profile** that includes a subset of modules (Catalogue replica, Inventory, Sales, Clinical, Procurement receiving, Customers credit cache, Reporting branch reports, Audit, Sync client) and excludes cloud-only modules (Platform, Finance ledger, Notification delivery, consolidated reporting).

Terminals are Next.js static exports (`output: 'export'`), client-rendered, with no server-side rendering or database access. They resolve their API base at run time: BLN on the LAN first, cloud second when online.

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Per-browser offline-first PWA with IndexedDB | IndexedDB storage is unreliable across browsers and devices; no single source of truth on the branch; every terminal syncs independently (more conflicts, more bandwidth); cannot enforce FEFO allocation across concurrent cashiers; 5 browsers × 500 products × prices × batches = significant duplication |
| Full cloud-only (no offline) | Unacceptable for the target market; a sale must never be blocked by an internet outage |
| Local PostgreSQL instance per branch | Possible but more heavyweight; the BLN already provides a single-writer embedded DB; a local PG may be chosen after the ADR-011 spike |

## Consequences

**Positive:**
- One authoritative branch database prevents conflicts between concurrent cashiers selling the same batch.
- FEFO allocation is accurate across all terminals (single-writer semantics on the BLN).
- Terminals are extremely lightweight; a low-spec Android phone with a browser can serve as a terminal.
- The BLN can serve the terminal bundle, so no CDN is required during outages.
- The sync surface is one BLN ↔ cloud channel per branch instead of N terminal ↔ cloud channels.
- Receipt printing and scanner integration happen through the BLN on the LAN.

**Negative / Risks:**
- Each branch requires a physical machine with a UPS; this is an operational cost.
- If the BLN fails, the branch cannot trade until it is restored (the terminal has no independent data). Mitigation: the BLN runs as an OS service with auto-restart; a nightly encrypted snapshot enables a machine rebuild from the cloud in under 2 hours; a redundant second BLN is a future option.
- The BLN installer, auto-update, and certificate enrolment must be built and maintained.
- The LAN must support HTTPS for camera barcode scanning (see ADR-013).

## References

- TDD §1.3, §8.1–8.7, §10.1, §10.3
- Plan §4.1, §4.5, §4.6, NFR A5
