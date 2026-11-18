# ADR-001 — Modular Monolith, Not Microservices

## Status

Accepted (Plan §5.5)

## Context

PharmaPOS must serve thousands of branches across multiple tenants with strong data isolation, offline capability, and complex transactional requirements (stock, payments, clinical dispensing). The engineering team is small at launch and will grow incrementally.

Microservices are frequently proposed for multi-tenant SaaS because they allow independent scaling. However, they introduce significant operational complexity: distributed transactions, service discovery, independent deployments per service, network latency between services, and the need for a mature DevOps capability from day one.

The alternative — a traditional layered monolith — risks becoming an unstructured "big ball of mud" over time, with hidden coupling between business domains that makes evolution difficult.

## Decision

Build a **modular monolith** using Spring Modulith. The codebase is organised into one top-level package per bounded context. Each module has:

- A public `api` package (facade interfaces, request/response types, event classes) that other modules may reference.
- An `internal` package that no other module may import.
- One table prefix and its own migration scripts.
- Clear publish/subscribe relationships via the domain event catalogue.

Spring Modulith's `@ApplicationModuleTest` and ArchUnit rules **fail the CI build** on any forbidden cross-module dependency or cycle, making boundary violations impossible to merge.

The two deployable configurations — **Cloud Core** and the **Branch Local Node (BLN)** — are assembly modules (`app-cloud`, `app-edge`) that select which domain modules and Spring profiles compose each executable. This delivers the operational simplicity of a monolith while preserving the team's ability to extract high-load modules later.

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Microservices from the start | Too much distributed systems complexity for the team size; distributed transaction saga for a sale is far harder to make correct than in-process calls; impossible to run on a branch edge device as separate services |
| Traditional layered monolith (no module boundaries) | No structural enforcement; coupling accumulates silently; known to become unmaintainable at the scale of 13 bounded contexts |
| Event-driven CQRS from day one | High implementation cost; premature for the current team and load; the outbox pattern achieves eventual consistency where needed without full CQRS |

## Consequences

**Positive:**
- Sale finalisation is a single in-process database transaction — no saga, no two-phase commit, no overselling risk from race conditions across services.
- The same domain code runs in the cloud and on the BLN, enforced by contract tests.
- Deployment is a single container per tier; no service mesh required at launch.
- Module boundaries are machine-enforced; the team can safely evolve toward service extraction when load justifies it.
- Much faster local developer experience.

**Negative / Risks:**
- Scaling a single module independently is not possible without first extracting it. Mitigation: the sync gateway is explicitly identified as the first extraction candidate (Plan §5.5) and is built as a module with minimal coupling.
- A slow or buggy module can affect the whole process. Mitigation: request timeouts, circuit breakers, and health checks per module.
- Database is shared: a schema migration must be backward-compatible across all modules simultaneously. Mitigation: the expand-and-contract migration pattern is mandatory (§4.7).

## References

- TDD §2.1, §2.3, §3.1–3.2
- Plan §5.5
