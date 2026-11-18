# PharmaPOS Enterprise Pharmacy Management System

> **Draft v0.1 · November 2026** — Companion to the Technical Design Document (TDD) and Product, Security & Implementation Plan.

A multi-tenant, offline-capable pharmacy management platform built for African markets, starting with Uganda. It serves community and institutional pharmacies with a cloud-hosted back-office and a **Branch Local Node (BLN)** that keeps every counter trading through internet outages.

---

## Monorepo Map

```
pharmapos/
├── contracts/            # Source of truth — OpenAPI specs and event schemas
│   ├── openapi/          #   pharmapos-api.yaml (all endpoints)
│   └── events/           #   JSON Schemas for domain events
│
├── backend/              # Spring Boot 4 modular monolith (Java 25)
│   ├── modules/          #   One sub-project per bounded context
│   │   ├── catalogue/
│   │   ├── inventory/
│   │   ├── sales/
│   │   ├── clinical/
│   │   ├── procurement/
│   │   ├── finance/
│   │   ├── customers/
│   │   ├── reporting/
│   │   ├── notifications/
│   │   ├── sync-gateway/
│   │   ├── audit/
│   │   ├── identity/
│   │   └── platform/
│   ├── app-cloud/        #   Assembly: cloud profile
│   └── app-edge/         #   Assembly: BLN edge profile
│
├── web/                  # Three Next.js applications
│   ├── terminal/         #   Static-export PWA for cashiers and pharmacists
│   ├── backoffice/       #   SSR back-office for owners and managers
│   ├── platform/         #   Platform console for support engineers
│   └── packages/         #   Shared: API client, UI kit, Zod schemas
│
├── installer/            # BLN packaging, auto-update, certificate enrolment
│
├── infra/                # Infrastructure as code, dev environment
│   └── docker/           #   docker-compose.yml for local development
│
└── docs/                 # ADRs, runbooks, design summaries
    └── adr/              #   Architecture Decision Records 001–017
```

---

## Technology Stack

| Concern | Choice |
|---|---|
| Runtime | Java 25 LTS |
| Framework | Spring Boot 4.1.x + Spring Security + Spring Data + Spring Modulith |
| Database | PostgreSQL 16 (managed, HA, PITR) with Row-Level Security |
| Migrations | Flyway (proposed — see ADR-014) |
| Identity | Keycloak 24 — two realms: `tenant` and `platform` |
| API | REST / JSON, OpenAPI 3.1, contract-first, versioned at `/api/v1/` |
| Async | Transactional outbox in PostgreSQL (no broker initially — ADR-008) |
| Frontend | Next.js 15 (App Router), TypeScript, React, installable PWA |
| Local node store | Encrypted embedded DB (SQLite + SQLCipher-class — spike ADR-011) |
| Observability | OpenTelemetry traces + metrics + structured logs |
| Money | `bigint` minor units + 3-char currency code (ADR-009) |
| Primary keys | UUIDv7 — time-ordered, offline-generatable (ADR-009) |

---

## Quick Start (Local Development)

### Prerequisites

- Docker Desktop 4.x or Docker Engine 24+ with Compose v2
- Java 25 JDK (for backend development)
- Node.js 22 LTS (for frontend development)
- Git

### 1. Clone

```bash
git clone https://github.com/your-org/pharmapos.git
cd pharmapos
```

### 2. Start infrastructure services

```bash
cp infra/docker/.env.example infra/docker/.env
# Edit .env to set secrets (see the file for guidance)
docker compose -f infra/docker/docker-compose.yml up -d
```

This starts:
- **PostgreSQL 16** on port `5432` (databases: `pharmapos_pool`, `pharmapos_platform`)
- **Keycloak 24** on port `8080` (admin: see `.env`)
- **Redis 7** on port `6379`
- **MinIO** on port `9000` (console: `9001`)
- **MailHog** on port `1025` (SMTP) / `8025` (web UI)

Wait for health checks to pass:
```bash
docker compose -f infra/docker/docker-compose.yml ps
```

### 3. Run database migrations

```bash
# From the backend directory (once backend module exists):
./gradlew :app-cloud:flywayMigrate
```

### 4. Start the backend

```bash
./gradlew :app-cloud:bootRun --args='--spring.profiles.active=local'
```

The API is available at `http://localhost:8081`. Swagger UI: `http://localhost:8081/swagger-ui.html`.

### 5. Start a frontend

```bash
cd web/terminal
npm install
npm run dev
# Terminal app → http://localhost:3000

cd web/backoffice
npm run dev
# Back-office → http://localhost:3001
```

---

## Architecture Decision Records

All decisions are in [`docs/adr/`](docs/adr/README.md). Key decisions:

| ADR | Decision |
|---|---|
| [ADR-001](docs/adr/ADR-001-modular-monolith.md) | Modular monolith, not microservices |
| [ADR-002](docs/adr/ADR-002-hybrid-tenancy.md) | Pooled schema with RLS; dedicated DB on demand |
| [ADR-004](docs/adr/ADR-004-bln-thin-terminals.md) | Branch Local Node + thin terminals |
| [ADR-005](docs/adr/ADR-005-event-sync.md) | Append-only, hash-chained event sync |
| [ADR-009](docs/adr/ADR-009-money-keys.md) | Money as integer minor units; UUIDv7 keys |

---

## Contributing

1. Read the [TDD summary](docs/TDD-summary.md) and the relevant ADRs before making changes.
2. Module boundaries are enforced by Spring Modulith — do not import from another module's `internal` package.
3. Every new table must have `tenant_id` and an RLS policy (the CI build fails without one).
4. Every endpoint must carry a permission annotation (the CI build fails without one).
5. Money arithmetic uses the `Money` value object; never use `double` or `float`.
6. All timestamps are UTC; device-local time is stored separately for audit only.

---

## Licence

Proprietary — © 2026 PharmaPOS Ltd. All rights reserved.
