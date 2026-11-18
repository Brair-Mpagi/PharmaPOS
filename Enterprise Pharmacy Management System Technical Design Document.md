# Enterprise Pharmacy Management System: Technical Design Document

Draft v0.1 · 18 November 2026 · Prepared for @Brair · Companion to the Product, Security & Implementation Plan (84 pages)

## 0. About this document

**Purpose.** The Product, Security & Implementation Plan (the Plan) says what to build and why. This Technical Design Document (TDD) says how: module boundaries, data model, API conventions, core transaction flows, the offline sync protocol, security implementation, deployment and testing. It is the document engineers build from. It is planning only; no code is written.

**Reading conventions.**

- References such as “Plan §5.5” point to the section of the Plan that holds the decision.
- **(proposed)** marks detail added here to make the design buildable. It is a recommendation, not yet a decision.
- **(spike)** marks an item that stays open until a Phase 0 spike reports.
- Where this TDD and the Plan disagree, the Plan wins until a decision record says otherwise.

**Audience.** Backend engineers, frontend engineers, the DevOps/security engineer, the QA lead, and technical reviewers or investors who need to judge feasibility.

**Status of the design.** Parts 1 to 3 restate decisions already taken in the Plan. Parts 4 to 15 add engineering detail; most of it is (proposed) and gets confirmed during P0 to P2. Part 16 lists the technical documents still to be written, and Part 17 lists open decisions.

## 1. Scope and system context

### 1.1 In scope and out of scope

| In scope (MVP and first commercial release) | Out of scope for now |
| --- | --- |
| Catalogue, batch inventory, FEFO, recalls, counts, transfers | Inpatient and hospital pharmacy workflows (Plan A3) |
| POS with offline mode, shifts, cash reconciliation | Full accounting package (export and integration instead, Plan §1.3) |
| Prescriptions and dispensing, controlled register | Microservices (revisit on measured need, Plan §5.5) |
| Procurement, supplier ledger, operational finance | Serial-level pack verification (future, Plan §4.9, §7.2) |
| Multi-branch, tenants, subscriptions, entitlements | E-prescription exchange and insurer claims (future) |
| Mobile scanning, reports, notifications, integrations | Other countries (regulatory-profile mechanism keeps the door open) |

### 1.2 Actors and external systems

| Actor or system | Interaction |
| --- | --- |
| Cashier, pharmacist, storekeeper | Terminal app on a counter PC or phone, talking to the Branch Local Node (BLN) over the LAN |
| Owner, branch manager, accountant | Back-office app in the cloud |
| Platform administrator, support engineer | Platform console, separate network path and identity realm |
| Identity provider (Keycloak or equivalent) | OIDC login, MFA, sessions; two realms: tenant users and platform staff |
| Mobile money aggregator | Collect and confirm payments (Plan A5); online only |
| URA EFRIS | Fiscal e-invoicing for tenants where required (Plan A7, §7) |
| SMS and email providers | Notifications, one-time codes as fallback |
| Object storage and key management service | Attachments, exports, backups; per-tenant data keys |

### 1.3 Deployable units

| Unit | What it is | Where it runs |
| --- | --- | --- |
| Cloud Core | Spring Boot modular monolith, cloud profile | Containers, two or more instances behind a load balancer |
| Branch Local Node (BLN) | Same codebase, edge profile, embedded encrypted database, auto-updating installer | One reliable machine per branch (UPS advised) |
| Terminal app | Next.js static export, client-rendered PWA | Served by the BLN on the LAN; same bundle served from a CDN when online |
| Back-office app | Next.js with server rendering | Cloud, behind the edge |
| Platform console | Next.js, separate deployment | Cloud, separate network path |
| Keycloak | OIDC provider, two realms | Cloud |
| PostgreSQL | Pool database plus optional silo databases per tenant | Managed, high availability, point-in-time recovery |
| Cache and rate limiting | Small Redis-compatible instance, or database counters to start | Cloud |
| Object storage and KMS | S3-compatible storage; managed key service or HSM | Cloud |

### 1.4 Trust zones (Plan §5.5)

1. **Internet edge:** WAF and TLS termination.
2. **Application tier:** Cloud Core, Next.js servers, sync gateway.
3. **Data tier:** PostgreSQL and object storage, no public access.
4. **Key and secrets zone:** KMS and secrets manager, separate administrators.
5. **Build and deployment zone:** CI/CD with its own credentials, separate from runtime.
6. **Branch zone:** the BLN and terminals, treated as semi-trusted: every event they send is validated by the cloud.
7. **Control plane:** platform console and platform realm, separate network path.

## 2. Technology stack and decision records

### 2.1 Stack (from Plan §5.5)

| Concern | Choice | Notes |
| --- | --- | --- |
| Runtime | Java 25 LTS | Current LTS |
| Framework | Spring Boot 4.1.x with Spring Security, Spring Data, Spring Modulith | No LTS line; plan a twice-yearly upgrade cadence and confirm versions on the official site at kickoff |
| Database | PostgreSQL, managed, HA, PITR | Row-Level Security, JSONB, partitioning |
| Migrations | Flyway or Liquibase, run by the pipeline | See ADR-014 |
| Identity | Keycloak or comparable OIDC provider | No hand-built authentication |
| API | REST over HTTPS, versioned, OpenAPI contract | Idempotency keys on mutating calls |
| Async | Transactional outbox in PostgreSQL plus a database-backed job scheduler | No message broker until a measured need |
| Search | PostgreSQL full-text and trigram indexes | Local index on the BLN |
| Frontend | Next.js (App Router), TypeScript, React, installable PWA | Two applications plus platform console (Plan §5.7) |
| Local node store | Embedded encrypted database (SQLite with SQLCipher-class encryption is the working assumption) | **(spike)** against a local PostgreSQL option |
| Observability | OpenTelemetry, metrics, structured logs with tenant-aware redaction | Needed to prove the SLOs |
| Deployment | Containers on a managed service, Kubernetes only if the team can run it; infrastructure as code | Start with two instances behind a load balancer |

### 2.2 Decision records

Each decision gets a short architecture decision record (ADR) in the repository: context, decision, alternatives, consequences.

| ADR | Decision | Status |
| --- | --- | --- |
| 001 | Modular monolith, not microservices | Accepted (Plan §5.5) |
| 002 | Hybrid tenancy: pooled schema with RLS by default, dedicated database on demand | Accepted (Plan §5.1) |
| 003 | PostgreSQL as system of record | Accepted |
| 004 | Branch Local Node with thin terminals; per-browser caching rejected as primary | Accepted (Plan §4.1) |
| 005 | Append-only, idempotent, hash-chained event sync | Accepted (Plan §4.5) |
| 006 | Next.js: terminal is a static export; back-office may use SSR; Spring Boot holds all business rules | Accepted (Plan §5.7) |
| 007 | Keycloak-class OIDC provider, separate realms for tenants and platform staff | Accepted (Plan §6.2) |
| 008 | Transactional outbox, no broker initially | Accepted |
| 009 | Money as integer minor units plus currency code; UTC timestamps; UUIDv7 keys | Accepted (Plan §5.6) |
| 010 | Stock exists only as batch balances; balance equals sum of immutable movements | Accepted (Plan §2.3) |
| 011 | Local database engine for the BLN | Open, P0 spike |
| 012 | Service-worker library (Serwist or hand-written Workbox) | Open |
| 013 | Certificate authority approach for HTTPS on the branch LAN | Open, P0 spike, mandatory |
| 014 | Flyway or Liquibase | Open (proposal: Flyway, see 4.7) |
| 015 | Hosting region and data residency | Open, Legal (Plan A6) |
| 016 | Job scheduler library | Open (proposed candidates: db-scheduler, JobRunr) |
| 017 | Default stock valuation (weighted average versus FIFO by batch) | Open, Product (Plan §2.3, §2.12) |

### 2.3 Repository layout (proposed)

One monorepo so contracts, backend and frontends version together.

| Path | Contents |
| --- | --- |
| `contracts/` | OpenAPI specification, sync event schemas, shared error codes: the source of truth for generated clients |
| `backend/` | One Gradle (or Maven) build with one module per bounded context, plus `app-cloud` and `app-edge` assembly modules that select which modules and configuration form each deployable |
| `web/terminal`, `web/backoffice`, `web/platform` | The three Next.js applications sharing a `web/packages` library (API client, UI kit, validation schemas) |
| `installer/` | BLN packaging, auto-update, certificate enrolment |
| `infra/` | Infrastructure as code, environment definitions, observability dashboards |
| `docs/` | ADRs, runbooks, this TDD |

## 3. Module design

### 3.1 Boundary rules

- One top-level package per bounded context. Each has a public `api` package (facade interfaces, request and response types, event classes) and an `internal` package nobody else may import.
- Spring Modulith verification runs in CI and **fails the build** on a forbidden dependency or a cycle.
- A module never reads or writes another module’s tables. Cross-module references hold only an ID, never a mapped entity.
- Each module owns a table prefix and its migration scripts.
- Public facades return plain data objects, never persistence entities.

### 3.2 Module catalogue

| Module | Owns (table prefix) | Publishes | Consumes |
| --- | --- | --- | --- |
| Platform (control plane) | `plt_`: tenant, subscription, plan, entitlement, support grant | TenantProvisioned, SubscriptionChanged | none |
| Identity and access | `iam_`: user, role, permission, role assignment, device, session | UserDisabled, DeviceRevoked | TenantProvisioned |
| Catalogue | `cat_`: product, sellable item, barcode, price list, tax class | ProductChanged, PriceChanged | none |
| Inventory | `inv_`: batch, movement, balance, count, transfer, adjustment | StockMoved, BatchStatusChanged | GoodsReceived, SaleCompleted (edge reconciliation) |
| Sales (POS) | `sal_`: sale, sale line, payment, refund, shift, cash movement | SaleCompleted, ShiftClosed | BatchStatusChanged, PriceChanged |
| Clinical | `rx_`: patient, prescription, dispensing, controlled register | DispensingRecorded | BatchStatusChanged |
| Procurement | `prc_`: supplier, PO, GRN, supplier invoice, supplier return | GoodsReceived | none |
| Finance | `fin_`: ledger account, journal entry and line, expense, reconciliation | EntryPosted | SaleCompleted, GoodsReceived, ShiftClosed, BalanceChanged |
| Customers | `cus_`: customer, account entry | BalanceChanged | SaleCompleted |
| Reporting | `rpt_`: read models and summary tables | none | most events |
| Notifications | `ntf_`: rules, deliveries, escalations | none | BatchStatusChanged, DeviceRevoked, exceptions |
| Sync gateway | `syn_`: device event, cursor, conflict record | EventApplied | all domain events destined for branches |
| Audit | `aud_`: tamper-evident audit log | none | every module’s audit calls |

### 3.3 Layering inside a module

1. **Web layer:** controllers, request and response DTOs, input validation, and a mandatory authorisation annotation on every endpoint (the build fails if one is missing, Plan §5.4).
2. **Application layer:** use-case services. This is the **transaction boundary** and where idempotency, permission checks and audit calls happen.
3. **Domain layer:** aggregates, value objects (Money, Quantity, Batch, Expiry), policies and state machines. No framework types.
4. **Persistence layer:** repositories and mappers. Only this layer touches SQL.

### 3.4 Communication patterns

| Flow | Pattern | Why |
| --- | --- | --- |
| Sale finalisation: allocate batches, decrement stock, record payment, write audit | Synchronous in-process calls inside **one** database transaction | Must be atomic; no overselling |
| SaleCompleted to finance posting, customer balance, report read models | Outbox event, eventually consistent | Can lag without harming trading |
| BatchStatusChanged (recall, quarantine) to branches | Outbox event, then sync gateway pull | Must reach every branch, including offline ones |
| Notifications and SMS | Outbox event plus retry | Delivery can fail independently |
| Heavy reports | Read replica or summary tables, asynchronous job | Keeps the sale path fast |

The outbox row is written in the **same transaction** as the state change. A relay reads unsent rows, publishes in-process, marks them sent, and retries with back-off. Poison messages go to a dead-letter table with an alert. Every consumer is idempotent.

### 3.5 Edge profile composition

The BLN runs the same domain code with a different assembly.

| Included on the BLN | Excluded from the BLN |
| --- | --- |
| Identity (local credential cache and device lease), Catalogue (replica), Inventory, Sales, Clinical (this branch, non-controlled by default), Procurement (receiving only), Customers (credit cache), Reporting (branch reports), Audit, Sync client | Platform, Finance ledger (posted in the cloud from synced events), Notification delivery, consolidated reporting, any price, role, user or device administration |

A contract test runs the same domain scenarios against both profiles, so the cloud and the BLN cannot drift on stock or sale rules.

## 4. Data architecture

### 4.1 Conventions (Plan §5.6, with proposed column standards)

- Primary keys are time-ordered UUIDs (UUIDv7). Terminals and the BLN may generate keys offline.
- Every business table carries `tenant_id`, and `branch_id` where branch-owned.
- Standard audit columns **(proposed)**: `created_at`, `created_by`, `updated_at`, `updated_by`, `version` (optimistic locking).
- Money is `bigint` minor units plus `currency` (3 characters). UGX has no minor unit in practice, so the amount is stored in whole shillings.
- Quantities are held in the item’s **base unit**. Storage type **(proposed)**: `numeric(14,3)`, to allow millilitre-level dispensing; decision to confirm in P2.
- Timestamps are UTC. Device time is stored separately for audit.
- No hard deletes of financial, stock or clinical records. Status changes and reversing entries instead.

### 4.2 Core tables (proposed column design)

**inv\_stock\_batch**

| Column | Type | Notes |
| --- | --- | --- |
| id, tenant\_id, branch\_id | uuid |  |
| sellable\_item\_id | uuid |  |
| batch\_number | text | Mandatory for regulated items |
| expiry\_date | date | Mandatory for regulated items; rejected if before today at receipt |
| supplier\_id, grn\_line\_id | uuid | Traceability back to the invoice |
| unit\_cost\_minor | bigint | Feeds COGS |
| status | enum | Available, Reserved, Quarantined, Expired, Recalled, Damaged |
| UNIQUE |  | (tenant\_id, branch\_id, sellable\_item\_id, batch\_number, supplier\_id) |

**inv\_stock\_movement** (immutable, partitioned by month)

| Column | Type | Notes |
| --- | --- | --- |
| id, tenant\_id, branch\_id, batch\_id | uuid |  |
| movement\_type | enum | RECEIPT, SALE, RETURN, TRANSFER\_OUT, TRANSFER\_IN, ADJUSTMENT, DAMAGE, LOSS, EXPIRY\_WRITE\_OFF, QUARANTINE, RELEASE |
| quantity\_delta | numeric(14,3) | Signed, in base units |
| source\_doc\_type, source\_doc\_id | text, uuid | Sale line, GRN line, count line, etc. |
| reason\_code, note | text | Mandatory for adjustments and overrides |
| occurred\_at, recorded\_at | timestamptz | Device time and server time |
| origin\_device\_id, origin\_seq | uuid, bigint | Present for edge-origin movements |

**inv\_stock\_balance** is a projection: one row per batch, `quantity_on_hand` and `quantity_reserved`, rebuilt from movements by a nightly job. Updates are made in the same transaction as the movement insert.

**sal\_sale** and **sal\_sale\_line**

| Column | Notes |
| --- | --- |
| `sal_sale`: id, tenant\_id, branch\_id, shift\_id, device\_id |  |
| receipt\_number | Sequential per device and branch |
| idempotency\_key | UUIDv7 generated by the terminal; unique per tenant |
| status | DRAFT, SUSPENDED, COMPLETED, VOIDED |
| total\_minor, tax\_minor, discount\_minor, currency |  |
| customer\_id, prescription\_id | Nullable; required for credit sale or prescription-linked sale |
| `sal_sale_line`: sale\_id, sellable\_item\_id, batch\_id | Batch allocated by FEFO |
| unit, quantity, unit\_price\_minor, unit\_cost\_minor, tax\_class, line\_discount\_minor, override\_reason | Override reason required when FEFO is overridden |

**sal\_payment:** sale\_id, method (CASH, MOBILE\_MONEY, CARD, CREDIT), amount\_minor, state (PENDING, CONFIRMED, FAILED, REVERSED), provider\_reference, confirmed\_at.

**aud\_event** and **syn\_device\_event** are described in Parts 8 and 9.

### 4.3 Stock integrity rules

1. A batch balance equals the sum of its movements. The nightly reconciliation job compares both and alerts on any drift (Plan NFR D1).
2. **Cloud-origin sales** (online terminals with no BLN in the path) lock the batch balance row (`SELECT ... FOR UPDATE`), reject any sale that would make it negative, and commit the movement and balance together.
3. **Edge-origin sales** are facts that already happened. The cloud applies them even if the balance goes negative, then raises a negative-stock exception for a manager to explain and adjust (Plan §4.6). So the cloud schema carries **no hard CHECK** on balance for edge-applied events; the guard lives in the application and the exception queue.
4. Returned medicines go to **Quarantined**, never straight to Available.

### 4.4 Row-Level Security design

Every table with a `tenant_id` gets the same policy pair, generated by a migration helper rather than written by hand.

```sql
ALTER TABLE sal_sale ENABLE ROW LEVEL SECURITY;
ALTER TABLE sal_sale FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON sal_sale
  USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
  WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
```

- The application sets the tenant at the start of every transaction with `set_config('app.tenant_id', :tenant, true)` (transaction-local), so a pooled connection can never carry one tenant’s context into another request.
- A missing setting yields NULL, which matches no rows: the system **fails closed**.
- Roles **(proposed)**: `app_rw` (no BYPASSRLS, not the table owner), `app_ro_reports` (for replicas), `migrator` (owner, used only by the pipeline), `platform_admin` (control-plane tables only).
- Branch-owned sensitive tables (clinical, controlled register) add a second policy on `branch_id` against `app.branch_ids`.

### 4.5 Indexing and partitioning (proposed)

| Object | Strategy |
| --- | --- |
| `inv_stock_movement`, `aud_event` | Range-partition by month; keep recent partitions on fast storage; archive old partitions but keep them queryable |
| Barcode lookup | Unique index on (tenant\_id, code) for active barcodes; resolves GTIN to sellable item in one probe |
| Product search | Trigram index on name and active ingredient plus full-text index; same definitions on the BLN |
| FEFO allocation | Index on (tenant\_id, branch\_id, sellable\_item\_id, status, expiry\_date) so the earliest-expiring Available batch is a single index range read |
| Reports | Summary tables per branch per day, refreshed by outbox consumers |

### 4.6 Capacity model (from Plan NFR S1 and P6)

1,000 tenants, 3,000 branches, 100,000 items per tenant and 50 million stock movements a year platform-wide, with 100 sales a second sustained at launch and a 3x burst. At about 12 bytes of key and index overhead per movement row, 50 million rows a year is manageable on one PostgreSQL primary with monthly partitions; the first scaling steps are read replicas for reports and then moving the largest tenants to silo databases.

### 4.7 Migrations

- Schema changes are versioned scripts run **only by the pipeline** under the `migrator` role. The application role cannot alter schema.
- **Expand and contract:** a release adds new structures first, the code uses both, and removal ships a release later, so every release is backward compatible and a rollback under 15 minutes is possible (Plan NFR M4).
- The pipeline loops over the tenant directory so the same migration lands in the pool database and every silo database.
- A build-time check fails if a new table lacks `tenant_id` and an RLS policy (Plan §5.4, test 6).
- Proposal: Flyway, for simple reviewable SQL files, unless a spike shows a need for Liquibase features (ADR-014).

## 5. Multi-tenancy implementation

### 5.1 Request flow

1. The request arrives at the edge with a bearer token and a tenant sub-domain.
2. The gateway validates the token (signature, expiry, audience) and checks that the **tenant in the token matches the tenant in the host**. A mismatch is rejected. A tenant identifier in a body, query or header is never trusted.
3. A request-scoped `TenantContext` (tenant id, branch grants, user id, roles, device id) is built once and is immutable for the request.
4. The application layer opens a transaction. The first statement sets `app.tenant_id` (and `app.branch_ids`) transaction-locally. All repository calls run inside that transaction.
5. Permission checks run in the application layer against the context. The database RLS policy is a second, independent barrier.

### 5.2 Tenant directory and data-source routing

| Element | Design (proposed) |
| --- | --- |
| Tenant directory | A control-plane table mapping `tenant_id` to its data source (pool cluster, or a silo database), status (active, read-only, locked, offboarding) and key reference |
| Routing | A routing data source looks up the tenant in a short-lived cache and returns the pool or silo connection pool. Application code never sees which one it got |
| Pool and silo parity | One migration pipeline, one codebase; only the data source differs |
| Pool to silo move | Export and import with checksums, verification, and a controlled cut-over window; rehearsed in P10 |
| Connection limits | Per-tenant concurrency caps on the shared pool to contain a noisy neighbour |

### 5.3 Everything that runs outside a request

| Surface | Rule |
| --- | --- |
| Background jobs and outbox consumers | The message carries tenant and branch; the worker rebuilds the context and sets the transaction-local tenant before touching data |
| Cache | Tenant id is part of every cache key; tests seed identical keys in two tenants |
| Search | Queried only through tenant-scoped transactions |
| Exports and attachments | Generated by tenant-scoped jobs; stored under a tenant-specific object-storage prefix; encrypted with that tenant’s data key |
| Integrations | Credentials held per tenant in the secrets manager; connectors never share a client |
| Logs and traces | Tenant id on every record; sensitive fields redacted before leaving the process |

### 5.4 Entitlements and configuration

- A central `EntitlementService` answers “may this tenant use feature X, add another branch, or exceed N users?”. It is called **in the backend at the point of action**, never only in the UI, and caches the answer briefly.
- Subscription state (active, grace, read-only, locked) feeds entitlements. A locked tenant’s offline leases are not renewed.
- Tenant settings (tax rules, receipt layout, approval caps, lease length, enabled features, regulatory profile) are stored as **versioned configuration**. Every change is audited and old versions stay readable so a past sale can be explained under the rules in force that day.

### 5.5 Regulatory profile mechanism (Plan §7.2)

A versioned configuration object selected per tenant and country: medicine classes, prescription validity, register formats, retention periods, offline allowances, required roles per class. No regulatory constant is hard-coded; domain policies read the active profile. This is how the 2026 regulations, or another country’s rules, are absorbed without a code rewrite.

## 6. API design

### 6.1 Conventions (proposed unless stated)

| Topic | Convention |
| --- | --- |
| Style | REST over HTTPS, JSON, OpenAPI contract first (Plan §5.5); clients are generated from the contract |
| Versioning | Major version in the path (`/api/v1/...`); additive changes only within a major version; deprecation window announced in the contract |
| Resource naming | Plural nouns; actions that are not CRUD use explicit sub-resources, for example `POST /sales/{id}/payments`, `POST /batches/{id}/quarantine` |
| Idempotency | Every mutating call accepts an `Idempotency-Key` header (UUIDv7). The server stores the key with a request fingerprint and the result for at least 30 days (Plan NFR D3). A replay returns the original result; the same key with a different body returns 422 |
| Concurrency | `ETag` and `If-Match` using the row `version` for updates to shared records |
| Pagination | Cursor-based, with a maximum page size; stable ordering key |
| Errors | One envelope: `code` (stable machine value), `message`, `details`, `traceId`. Codes live in the contract and are shared with the frontends |
| Authorisation | Every endpoint carries a permission annotation; the build fails without one |
| Rate limiting | Per user, per device, per tenant, per source address; stricter on login, export and report endpoints |
| Time and money | ISO-8601 UTC; money as `{amountMinor, currency}` |
| Tracing | W3C `traceparent` accepted and propagated; `traceId` returned on errors |

### 6.2 Endpoint groups

| Group | Representative operations | Main permission |
| --- | --- | --- |
| Catalogue | search items, get by barcode, create or version a sellable item, map a barcode, manage price lists | catalogue.read, catalogue.write, price.change |
| Inventory | list batches, receive stock, adjust, quarantine or release a batch, start and submit a count, send and receive a transfer | stock.read, stock.adjust, stock.count, stock.transfer |
| Sales | open and close shift, create and suspend a sale, add lines, add payments, finalise, void, refund, reprint | sales.create, sales.refund, sales.discount, shift.manage |
| Clinical | register, verify and dispense a prescription, partial fill, controlled-register entry | rx.register, rx.verify, rx.dispense, rx.controlled |
| Procurement | requisition, purchase order and approval, goods received note, supplier invoice, supplier return | proc.order, proc.approve, proc.receive |
| Finance | expenses, cash movements, journal view, reconciliation, tax export | fin.read, fin.expense, fin.reconcile |
| Customers | customer and patient profiles, account entries, credit limit | cust.read, cust.write, cust.credit |
| Reporting | run or schedule a report, download an export | report.run, export.large |
| Identity | users, roles, assignments, device enrolment and revocation | iam.manage, device.manage |
| Sync (device only) | push events, pull changes, renew lease, report status | device certificate only |
| Platform (control plane) | tenants, subscriptions, plans, support grants | platform realm only |

The permission names above are working labels; the authoritative permission matrix is Plan §3.2.

### 6.3 Contract rules for the terminal

- The terminal generates the sale’s idempotency key before the first attempt, so a lost response followed by a retry can never create a second sale.
- Finalising a sale is a **single command** that returns the completed sale, its receipt number and any warnings (short-dated batch, price variance). The client does not orchestrate stock and payment as separate calls.
- The terminal resolves its API base at run time: BLN first, cloud second (Plan §5.7).

## 7. Core transaction designs

### 7.1 Finalise a sale (BLN and cloud, same logic)

1. **Validate** the request: idempotency key unseen (or return the stored result); shift open; device authorised and lease valid; caller holds the permission.
2. **Price and tax:** price from the price list in force, tax from each item’s tax class. Reject a line below cost unless the caller has the permission.
3. **Prescription gate:** every prescription-only line needs a verified linked prescription, or a pharmacist override with a reason where the regulatory profile allows it.
4. **Allocate batches (FEFO):** for each line, take the earliest-expiring Available batch with enough quantity; split across batches when needed; refuse expired, quarantined or recalled batches. A cashier override needs the permission and a reason.
5. **Lock and decrement:** on the cloud, lock the affected balance rows in a consistent order (batch id ascending) to avoid deadlocks; on the BLN, single-writer semantics make the lock implicit. Insert one SALE movement per allocation.
6. **Payments:** verify payments cover the total. Cash and credit are final at once. Mobile money and card payments must be **confirmed** by the provider before the sale is final when online; they are not available offline.
7. **Persist** the sale, lines, payments, receipt number and the idempotency record.
8. **Audit and outbox:** write the audit record and the SaleCompleted event in the same transaction.
9. **Commit**, then return the completed sale. On the BLN the event also enters the sync outbox.

A failure at any step rolls back the whole transaction. A sale is never left half-finished.

### 7.2 FEFO allocation algorithm (proposed)

Input: sellable item, requested quantity in base units, branch, current date. Output: a list of (batch, quantity) pairs.

1. Query Available batches for the item and branch with `expiry_date >= today`, ordered by `expiry_date`, then `created_at`.
2. Walk the list, taking `min(remaining, available)` from each batch until the request is met.
3. If the list runs out with quantity still owed, return a **shortfall error** with the maximum available. Never sell beyond available batch stock.
4. If any chosen batch is inside the short-dated threshold, attach a warning.
5. Reserved quantity (suspended sales, pending transfers) is subtracted from available before allocation.

### 7.3 Receiving stock

1. Scan a plain EAN to select the product, or a GS1 DataMatrix to fill product, batch and expiry (Plan §4.9).
2. Validate against the purchase order and catalogue: batch and expiry mandatory for regulated items; expiry not in the past; a duplicate batch number for the same product and supplier prompts confirmation.
3. Create the batch row, a RECEIPT movement and the GRN line in one transaction.
4. Publish GoodsReceived; finance posts the accrual and supplier ledger entry asynchronously.

### 7.4 Transfer between branches (state machine)

DRAFT, SENT (stock leaves the sender into **In-Transit**), RECEIVED (the receiver confirms quantities), CLOSED, with DISPUTED when quantities differ and CANCELLED before sending. Both sides are event-sourced. Only the receiving side can confirm receipt. An unmatched transfer raises an alert after a configurable timeout. Sending works offline; the receipt is confirmed by the receiving branch.

### 7.5 Refund and return

1. Reference the original sale where possible; refund to the original method by default.
2. Cash refunds above the configured limit need a supervisor credential (a PIN offline, a normal approval online).
3. The returned stock is recorded as a RETURN movement into **Quarantined**; a pharmacist inspection releases it or writes it off.
4. For a fiscal tenant, a credit note is issued against the original fiscal document (Plan §7.2).

### 7.6 Recall

A recall by batch identifiers freezes every matching batch across all the tenant’s branches (status Recalled, still counted but not sellable), lists every sale and dispensing record linked to those batches, and opens tasks for customer notification and recovery. Branches that are offline receive the status on their next pull; any sale made in between is accepted and listed on the recall follow-up (Plan §4.6).

### 7.7 Stock count

Counts are blind: the counter never sees the system quantity. Lines are submitted, variances computed against the movement-derived balance as of a recorded cut-off, and anything above the threshold waits for approval. Approved variances post as ADJUSTMENT movements with a reason; sales continuing during the count are handled by the cut-off timestamp.

## 8. Branch Local Node and synchronisation design

### 8.1 BLN architecture

| Element | Design |
| --- | --- |
| Process | Spring Boot edge profile, started as an operating-system service with automatic restart |
| Local store | Encrypted embedded database (SQLite with SQLCipher-class encryption as the working assumption, **spike** against local PostgreSQL); write-ahead logging; single writer |
| Local services | HTTPS server on the LAN for terminals, phones and printers; static terminal bundle; local catalogue index in memory |
| Journal | An append-only event journal written to a **second location** (separate disk or removable drive) |
| Snapshots | Nightly encrypted snapshots; restore plus journal replay, then a full resync check |
| Self-check | Integrity check at start-up; the node refuses to trade until it passes or a manager acknowledges a degraded mode |
| Updates | Signed update packages; the terminal bundle and node release version together so they stay compatible |
| Monitoring | Heartbeat to the cloud (version, lease time left, pending-event count, disk health, clock skew); local health page |

### 8.2 Key and lease hierarchy

| Key or credential | Purpose | Lifetime |
| --- | --- | --- |
| Tenant key (KMS-wrapped) | Root for the tenant’s data keys; supports crypto-shredding | Rotated on schedule |
| Device key pair and certificate | Mutual TLS to the sync gateway; signs each event | Issued at enrolment; revoked centrally |
| Lease | Signed statement of what the node may do and until when; carries a wrapped key | Default 72 hours, configurable to a ceiling of 7 days; 24 hours for high-risk roles; capped at 5 days for fiscal (EFRIS) tenants |
| Local database key | Encrypts the local store | Wrapped by a key released with each lease and held in the operating-system key store; unreadable after lease expiry without contacting the cloud |
| Local credentials | Cashier PIN or password verified offline | Argon2id hash with per-user salt; lockout and delay |

When the lease expires the node enters **restricted mode**: lookup and read-only views, no new sales.

### 8.3 Event envelope (proposed)

Every state change on the BLN is recorded as one immutable event.

| Field | Meaning |
| --- | --- |
| `eventId` | UUIDv7; also the idempotency key |
| `deviceId`, `deviceSeq` | Device and its strictly increasing sequence number |
| `tenantId`, `branchId` | Scope; the cloud verifies them against the certificate |
| `type`, `schemaVersion` | For example SaleCompleted v1, StockMoved v1, ShiftClosed v1 |
| `occurredAt`, `deviceClock` | Device time kept for audit; server time orders across devices |
| `payload` | The business data, validated against the contract schema |
| `prevHash`, `hash` | Hash of this event’s content chained to the previous event’s hash |
| `signature` | Device signature over the content and `prevHash` |

Deleting, reordering or editing any local event breaks the chain, which the cloud detects.

### 8.4 Sync protocol (Plan §4.5)

| Step | Endpoint (proposed) | Behaviour |
| --- | --- | --- |
| Push | `POST /sync/v1/events` | Batched, ordered, over mutual TLS. Server stores the `eventId`; a replay returns the original result |
| Validate | (server side) | Signature, chain continuity, device status, lease valid at event time, tenant and branch scope, business-rule validity |
| Apply | (server side) | Apply in device order inside transactions; per-event result: accepted, accepted with flag, rejected with reason |
| Pull | `GET /sync/v1/changes?cursor=...` | Catalogue, prices, users and permissions, batch status changes, approvals, notifications, configuration since the cursor |
| Reconcile | (server side) | Recompute stock projection; discrepancies become exception records, never silent corrections |
| Acknowledge | response to push | The BLN marks events synced **only after** the cloud acknowledges; unsynced events are retained |
| Lease | `POST /sync/v1/lease/renew` | Issue a fresh signed lease and key if the device and tenant are in good standing |
| Status | `POST /sync/v1/heartbeat` | Version, pending count, clock skew, integrity state |

After a long disconnection the BLN pushes in this priority order: shift closes and cash events, then sales and payments, then stock movements, then everything else. Sync is throttled and resumable from the last acknowledged `deviceSeq`.

### 8.5 Conflict handling (Plan §4.6)

Principle: a completed sale is a fact and is never silently rejected. The sync gateway records each conflict as an **exception task** with an owner and a due date. Last-writer-wins is **not** used for money or stock; it is allowed only for low-risk display preferences.

| Conflict | Handling |
| --- | --- |
| Sale of a batch recalled, quarantined or expired centrally | Accept; list affected units; open recall follow-up |
| Balance negative after merge | Accept; negative-stock exception; manager explains and adjusts with approval |
| Same prescription dispensed offline at two branches | Record both; duplicate-dispensing alert to pharmacists |
| Price changed centrally while the node sold at the old price | Accept inside the lease window; price-variance report |
| Catalogue edit in cloud versus node | Cloud is authoritative; the node may only draft barcode mappings pending approval |
| Event from a revoked device after the revocation time | Reject and quarantine for manual review |
| Duplicate event | Dropped by idempotency key; original result returned |
| Clock skew | Server time orders events; large skew is flagged |

### 8.6 Cloud-side sync gateway

A module inside the monolith (not a separate service yet) exposing the device endpoints on a distinct listener. Device events are written to `syn_device_event` first, then applied. It is the first candidate to extract if sync load needs independent scaling (Plan §5.5).

### 8.7 Spikes that gate the design (Phase 0)

| Spike | Question | Exit criterion |
| --- | --- | --- |
| S1 Local store | SQLite with SQLCipher-class encryption versus local PostgreSQL | Meets scan-to-line p95 300 ms and finalise-sale p95 1 s on reference hardware; supports encryption, snapshot and restore |
| S2 LAN HTTPS | Per-tenant private certificate authority enrolled on branch devices | Camera scanning works over HTTPS on the LAN on the supported phone list |
| S3 Sync soak | 72-hour disconnection with a realistic sale load, then reconnect | Zero lost or duplicated sales; priority order observed; reconciliation clean |
| S4 RLS | Pool tenancy under load with transaction-local tenant setting | No cross-tenant read in the generated isolation suite; acceptable overhead |
| S5 Hardware | Scanners, receipt printers, label printers | Published tested-device list |

## 9. Security implementation

The security requirements are set in Plan Part 6. This part says where each control lives in the system.

### 9.1 Authentication and sessions

| Concern | Implementation |
| --- | --- |
| Login | OIDC authorisation-code flow with PKCE against Keycloak; separate realms for tenant users and platform staff; no custom password storage in the application |
| Passwords | Minimum 12 characters, breached-password check, no forced periodic rotation; Argon2id with per-user salt |
| MFA | Mandatory for owners, administrators, accountants, approving pharmacists and all platform staff; WebAuthn or passkeys first, TOTP second, SMS only as a fallback that a tenant can disable |
| Cashier access | Username plus PIN or password on an enrolled device; idle lock after 5 minutes with PIN unlock; supervisor credential for high-risk actions |
| Tokens | Access token about 10 to 15 minutes; rotating refresh token bound to the device; server-side revocation list; absolute session limit 12 hours |
| Back-office session | HttpOnly, Secure, SameSite cookie issued by the Next.js server layer; strict Content-Security-Policy with per-request nonces |
| Terminal session | Short-lived token held in memory, never in localStorage |
| Throttling | Progressive delay per account and per source address; temporary, not permanent, lockout; alert to administrators |
| Offboarding | One command disables the user, revokes sessions, tokens and device grants, reassigns pending approvals and audits the action; effective at the next sync or lease expiry offline |

### 9.2 Authorisation enforcement

- **Model:** roles carry permissions; a role assignment is **per branch**; some permissions carry value limits (for example refund cap, discount percentage). The matrix is Plan §3.2.
- **Where enforced:** the application layer checks the permission, the branch grant and the value limit. Controllers carry the annotation; services perform the check. The frontend only hides what the caller cannot do and is never relied on.
- **Separation of duties** (Plan §3.4) is coded as explicit policy objects, for example: the creator of a purchase order cannot approve it; the person who counts stock cannot approve their own variance; a user cannot approve their own refund.
- **Platform boundary** (Plan §3.5): platform staff have no standing access to tenant business data. Support access is a **break-glass grant**: tenant-approved or dual-approved, time-limited, scoped, session-recorded and alerted, and visible to the tenant owner afterwards.
- **Permission tests:** a generated matrix test calls every endpoint as every role and compares the result to the expected matrix.

### 9.3 Tamper-evident audit log

| Element | Design (proposed) |
| --- | --- |
| What is recorded | Price change, refund, adjustment, dispensing, role change, discount over cap, FEFO override, reprint, export, login events, support access, configuration change, offline session start and end |
| Record | Who (user, device, branch), what (entity type and id, action), before and after values for changes, reason, source (terminal, back-office, sync), server time, device time, trace id |
| Append-only | The audit table has insert-only grants; no update or delete privilege for the application role |
| Hash chain | Each record stores the hash of its content plus the previous record’s hash within the tenant, so removal or alteration is detectable |
| Anchoring | A periodic digest of the chain head is written to separate write-once storage under a different credential |
| Verification | A scheduled job re-walks the chain and alerts on a break |
| Privacy | Patient identifiers appear only as references; sensitive values are masked or field-encrypted |

### 9.4 Data protection

| Topic | Control |
| --- | --- |
| In transit | TLS 1.2 minimum, 1.3 preferred; HSTS; automatic certificate renewal; mutual TLS between devices and the sync gateway; internal traffic encrypted |
| At rest | Encrypted database storage and backups; field-level encryption for patient identifiers, prescription content and credentials |
| Keys | Per-tenant data keys wrapped by the key service; envelope encryption; rotation schedule; key administrators separate from database administrators |
| Secrets | Secrets manager only; never in code, images or logs; short-lived service credentials |
| Database access | Private network only; no standing human access; break-glass with approval and session recording |
| Backups | Encrypted with separate keys; immutable or offline copy against ransomware; restore access limited to backup identities |
| Exports | Permission and approval for large exports; signed, expiring links; audit of every export |
| Deletion | Retention rules per record class from the regulatory profile; scheduled deletion with verification; **crypto-shredding** by destroying the tenant key at end of life; legal-hold support |

### 9.5 API and application hardening

- **Input:** validation at the edge of every endpoint against the contract schema; parameterised queries only; output encoding; size limits on bodies and uploads.
- **Uploads (prescription images, attachments):** type and size allow-lists, content sniffing, malware scanning, stored outside the web root under the tenant prefix, served through short-lived signed URLs.
- **Browser:** CSP with nonces, no inline scripts, frame-ancestors denied, CSRF protection for cookie-based sessions, subresource integrity on third-party scripts.
- **Object-level checks:** every lookup by ID is scoped by tenant and branch so guessing an identifier returns not-found.
- **Abuse controls:** rate limits, anomaly alerts, an export throttle, and a web application firewall at the edge.
- **Supply chain:** pinned dependencies and lockfiles, an SBOM per build, signed images, dependency and container scanning in CI; critical findings fixed in 7 days, high in 30 (Plan NFR M2).

The full threat model, with STRIDE-style analysis by component, is in Plan §6.9; the TDD adds a threat-to-control trace in the security design document (Part 16).

## 10. Frontend technical design

### 10.1 Three applications, one repository (Plan §5.7)

| Application | Rendering | Runs from | Key constraint |
| --- | --- | --- | --- |
| Terminal | Static export (`output: 'export'`), client-rendered PWA, service-worker cached | The BLN on the LAN; the same bundle from a CDN when online | Must load and run with no internet; no server-side rendering |
| Back-office | Server components and SSR allowed | Cloud, behind the edge | Online only |
| Platform console | Same framework, separate deployment and identity realm | Cloud, separate network path | Control-plane isolation |

### 10.2 Rules that keep the frontend safe

1. Spring Boot is the **only** place with business rules, authorisation and database access. Next.js server code handles session cookies, OIDC redirects and rendering, and holds no database credentials.
2. Never rely on Next.js middleware or route guards for authorisation. Keep Next.js patched; check current security advisories at kickoff and on every release.
3. The API client is generated from the OpenAPI contract, so frontend types cannot drift from the backend.
4. The terminal generates the idempotency key for each sale before the first attempt.

### 10.3 Terminal app design

| Concern | Design |
| --- | --- |
| State | Cart and UI state in memory, with drafts in IndexedDB (transient only); the BLN is the source of truth for stock, prices and sales |
| Data fetching | TanStack Query with explicit offline-aware retry rules; mutations carry idempotency keys |
| Forms | React Hook Form with Zod schemas aligned to the API contract |
| Scan input | A dedicated scan handler that detects the scanner’s prefix and suffix characters and keeps focus; handles USB and Bluetooth keyboard-wedge scanners |
| Camera scanning | Browser barcode detection where present, WebAssembly decoder as fallback; parses GS1 application identifiers 01, 10, 17 and 21; needs a secure context (HTTPS on the LAN) |
| Banners | Lease time remaining, pending-event count, catalogue staleness, restricted mode |
| Printing | Receipt and label output through the BLN or a browser print path; reprints marked COPY and audited |
| Accessibility | WCAG 2.2 AA; keyboard-first POS flows |
| Language | English first, with next-intl so local languages can follow |
| Performance budget | Initial JavaScript at or below 250 KB gzipped (proposed); scan-to-line target per Plan NFR P1 |

### 10.4 Back-office app design

Dashboards and reports use server components with data fetched through the same API. Large tables use cursor pagination and virtualised rendering. Report requests that exceed the interactive limit become asynchronous jobs with a notification and a signed download link (Plan NFR P7).

### 10.5 Frontend test and build tooling

Vitest for units, Playwright for end-to-end tests including **offline-mode scenarios**, axe for accessibility checks, lockfile with dependency scanning, pinned Node LTS.

Open frontend decisions: service-worker library (ADR-012), the LAN certificate approach (ADR-013) and whether the back-office is hosted as a container or on an edge platform.

## 11. Integrations

### 11.1 Connector pattern

Every external provider sits behind a **connector** with the same shape: a port (interface) owned by the domain, an adapter owned by the integration, credentials held per tenant in the secrets manager, a circuit breaker, a timeout and retry policy with idempotency, and a stored request and response log with sensitive fields redacted. A provider can be replaced without touching domain code.

### 11.2 Integration catalogue

| Integration | Direction | Technical notes | Phase |
| --- | --- | --- | --- |
| Mobile money (through an aggregator, Plan A5) | Collect and confirm | Payment starts as PENDING, moves to CONFIRMED on provider callback or poll; callbacks are signed and verified; idempotent by provider reference; online only | P8 |
| Card payments | Collect and confirm | Same state model; no card data stored or processed by the PMS (use a hosted provider page) | P8 |
| URA EFRIS (fiscal e-invoicing) | Issue and cancel | A fiscal document number attaches to a sale; a credit note on return; offline allowance of five days drives the lease cap for fiscal tenants. **Verify obligations and the current specification before design freeze** | P8 |
| SMS and email | Outbound | Notifications, escalations, fallback one-time codes; delivery receipts stored | P7 |
| Accounting export | Outbound | Signed, versioned file export of the ledger; direct API integrations later | P8 |
| Medicine reference dataset | Inbound | Licensed dataset in a shared read-only reference store; tenant additions never overwrite it | Optional |
| Public API for tenants | Inbound | Scoped API keys, rate limits, webhooks | P10 |

### 11.3 Reliability rules

- Calls to providers never run inside the sale transaction except where confirmation is mandatory, and then with a short timeout; a timeout leaves the payment PENDING and the sale unfinalised.
- Inbound webhooks are verified, stored first, and processed asynchronously and idempotently.
- A circuit breaker opens on repeated failures and the terminal shows a clear offline-payment message.
- A reconciliation job compares provider settlement reports with recorded payments daily and raises exceptions.

## 12. Infrastructure, delivery and operations

### 12.1 Environments

| Environment | Purpose | Data |
| --- | --- | --- |
| Local | Developer machines with containers for PostgreSQL and Keycloak | Synthetic |
| CI | Ephemeral, per build: unit, integration, isolation and contract tests | Synthetic |
| Staging | Production-like; full end-to-end, load and offline soak tests | Synthetic or masked |
| Pilot and production | Real tenants | Real; no production data in lower environments |

### 12.2 Cloud topology (proposed)

Two or more Cloud Core instances across availability zones behind a load balancer; a managed PostgreSQL primary with a synchronous standby in-region and continuous WAL archiving; read replicas for reports; Keycloak in high-availability mode; Redis-compatible cache; S3-compatible object storage; a key management service; a WAF at the edge. Everything is defined as infrastructure as code. Premium tiers add a warm standby for faster regional recovery (Plan NFR A4).

### 12.3 CI/CD pipeline

1. Build and unit tests, plus Spring Modulith boundary verification.
2. Static analysis, secret scanning, dependency and licence scanning, SBOM.
3. Integration tests against a real PostgreSQL, with the generated tenant-isolation suite (Plan §5.4).
4. Contract tests: backend against OpenAPI, frontends against the generated client, BLN and cloud against the same domain scenarios.
5. Build signed container images and the signed BLN installer.
6. Deploy to staging; run end-to-end, offline and performance smoke tests.
7. Manual approval gate; rolling deployment to production with health checks.
8. Automatic rollback on failed health checks; the target is rollback in under 15 minutes (Plan NFR M4).

The pipeline **blocks on critical findings** from P1 onward.

### 12.4 BLN fleet management

The BLN is part of the product, so it has its own operations: signed auto-updates with staged rollout and rollback, heartbeat monitoring, remote diagnostics that never expose patient data, a hardware specification with a UPS recommendation, and a scripted replacement procedure that rebuilds a branch machine from the cloud in under 2 hours (Plan NFR A5).

### 12.5 Observability

- **Telemetry:** OpenTelemetry traces, metrics and structured logs, with tenant id on every record and redaction of sensitive fields before export.
- **Business metrics:** sales per second, finalise latency, sync lag per device, pending-event counts, exception queue size, negative-balance count, ledger drift, outbox backlog.
- **Dashboards and alerts:** one dashboard per SLO in Part 14; alerts routed to an on-call rota with runbooks.
- **Health:** every module exposes liveness, readiness and business health checks.

### 12.6 Backup and disaster recovery (Plan §8.5)

| Layer | Mechanism | Target |
| --- | --- | --- |
| Cluster | Physical backups with continuous WAL archiving, point-in-time recovery | RPO 5 minutes |
| Tenant | Logical per-tenant export for tenant-level restore | Restore on request |
| Object storage | Versioning plus an immutable copy |  |
| BLN | Nightly encrypted snapshots, second-location journal, resync from cloud | Rebuild in 2 hours or less |
| Regional failure | Infrastructure as code rebuild or warm standby | RTO 4 hours; zone failure 30 minutes |
| Drills | Quarterly restore drills with evidence; annual full DR exercise | Part of the readiness checklist |

## 13. Test and quality design

### 13.1 Test layers

| Layer | Scope | Tooling (proposed) |
| --- | --- | --- |
| Unit and property | Domain rules on stock, money, FEFO, permissions; property tests assert that balance equals the sum of movements for random sequences | JUnit, a property-testing library |
| Mutation | Stock, money and permission logic at or above 70% mutation score; 80% line coverage on those modules (Plan NFR M1) | PIT or equivalent |
| Integration | Modules against a real PostgreSQL with RLS enabled | Testcontainers |
| Architecture | Module boundaries, endpoint annotations, table RLS policy present | Spring Modulith, ArchUnit |
| Tenant isolation | Generated: every endpoint and table attacked as another tenant; fail-closed check | Custom generator in CI |
| Permission matrix | Every endpoint as every role | Generated |
| Contract | OpenAPI conformance; consumer tests | OpenAPI tooling |
| End-to-end | Sale, return, dispensing, receiving, transfer flows in a browser | Playwright |
| Offline and sync | Disconnect and reconnect, lease expiry, clock skew, duplicate and reordered events, device revocation | Playwright plus a network-fault harness |
| Chaos and idempotency | Retries, dropped responses, process kills mid-sale: zero duplicate sales (Plan NFR D3) | Fault injection |
| Performance | Scan-to-line, finalise sale, search, report generation, sustained throughput | k6 or Gatling on reference hardware |
| Security | Static analysis, dependency scanning, DAST, and an external penetration test before launch focused on tenant isolation | CI scanners plus a third party |
| Accessibility | axe checks plus manual keyboard and screen-reader review | axe |
| Disaster recovery | Restore drills and BLN rebuild | Scheduled drills |

### 13.2 Critical scenarios (Plan §9.2)

The failure-scenario catalogue in the Plan becomes the regression pack. Scenarios 1 to 5 and 9 are acceptance criteria for P3, and each carries an automated test with a named owner. The pack runs nightly in staging and before every release.

### 13.3 Test data

A generator produces realistic Ugandan pharmacy catalogues (including plain EAN, GS1 and unlabelled packs), batches with mixed expiries, and multi-branch tenants. No production data is used outside production.
