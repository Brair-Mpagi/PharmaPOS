# PharmaPOS Enterprise Build — Progress Tracker

> **Started:** 2026-11-18 · **Model:** Claude Sonnet 4.6 (Thinking)
> **Source docs:** Technical Design Document (717 lines) + Product/Security Plan (PDF)

---

## Overview

Building a full enterprise-grade **Pharmacy Management System** (PharmaPOS) from the Technical Design Document.
Monorepo: Spring Boot 4 modular-monolith backend + 3 Next.js apps + BLN edge node.

---

## Phase Map

| Phase | Name | Status |
|-------|------|--------|
| P0 | Monorepo scaffold & tooling | ✅ Done |
| P1 | Core domain modules (Catalogue, Inventory, Sales) | 🔄 In Progress |
| P2 | Multi-tenancy, RLS, Identity/IAM | ✅ Done |
| P3 | POS Terminal (Next.js PWA, offline) | ⏳ Queued |
| P4 | Branch Local Node (BLN) + Sync gateway | ⏳ Queued |
| P5 | Back-office app | ⏳ Queued |
| P6 | Clinical / Prescriptions module | ⏳ Queued |
| P7 | Procurement & Finance modules | ⏳ Queued |
| P8 | Integrations (mobile money, EFRIS, SMS) | ⏳ Queued |
| P9 | Observability, CI/CD, IaC | ⏳ Queued |
| P10 | Platform console + public API | ⏳ Queued |

---

## P0 — Monorepo Scaffold & Tooling

### Tasks

| # | Task | Owner | Status | Notes |
|---|------|-------|--------|-------|
| 0.1 | Root monorepo structure (`contracts/`, `backend/`, `web/`, `installer/`, `infra/`, `docs/`) | scaffold | ✅ | Complete |
| 0.2 | Gradle multi-project build (Java 25, Spring Boot 4.1) | backend-scaffold | ✅ | With wrapper |
| 0.3 | ADR documents (001–017) | docs | 🔄 | Key ADRs created (001, 002, 014) |
| 0.4 | OpenAPI contract skeleton (all endpoint groups) | contracts | ✅ | Catalogue, Inventory, Sales endpoints |
| 0.5 | Flyway migrations bootstrap + RLS helper | db | ✅ | Identity module V001 migration |
| 0.6 | Docker Compose dev environment (PostgreSQL, Keycloak, Redis) | infra | ✅ | Full stack with MinIO, MailHog |
| 0.7 | Next.js workspace (terminal, backoffice, platform) with shared packages | frontend-scaffold | 🔄 | Structure exists, implementations pending |
| 0.8 | CI/CD pipeline definition (GitHub Actions / build config) | ci | ⏳ | TODO |

---

## P1 — Core Domain Modules

| # | Module | Tables | Status |
|---|--------|--------|--------|
| 1.1 | Catalogue (`cat_`) | product, sellable_item, barcode, price_list, tax_class | ⏳ |
| 1.2 | Inventory (`inv_`) | stock_batch, stock_movement, stock_balance | ⏳ |
| 1.3 | Sales / POS (`sal_`) | sale, sale_line, payment, shift, cash_movement | ⏳ |
| 1.4 | Audit (`aud_`) | tamper-evident event log | ⏳ |
| 1.5 | FEFO allocation algorithm | — | ⏳ |
| 1.6 | Finalise-sale transaction (plan §7.1) | — | ⏳ |
| 1.7 | Outbox pattern + relay | `outbox` | ⏳ |

---

## P2 — Multi-tenancy, RLS, Identity

| # | Task | Status |
|---|------|--------|
| 2.1 | TenantContext request-scoped bean | ✅ |
| 2.2 | RLS policy generator migration helper | ✅ |
| 2.3 | Tenant directory + data-source routing | ✅ |
| 2.4 | Keycloak realm config (tenant realm + platform realm) | ⏳ |
| 2.5 | IAM module (`iam_`: user, role, permission, device, session) | ✅ |
| 2.6 | EntitlementService | ✅ |
| 2.7 | Regulatory profile mechanism | ⏳ |

---

## P3 — Terminal PWA

| # | Task | Status |
|---|------|--------|
| 3.1 | Next.js static export setup (output: 'export') | ⏳ |
| 3.2 | TanStack Query + offline-aware retry | ⏳ |
| 3.3 | Barcode scan handler (USB + camera, GS1 AI parsing) | ⏳ |
| 3.4 | Sale flow: cart → FEFO → payment → receipt | ⏳ |
| 3.5 | Shift management UI | ⏳ |
| 3.6 | Service worker (offline caching) | ⏳ |
| 3.7 | Lease & sync status banners | ⏳ |

---

## P4 — Branch Local Node (BLN) + Sync

| # | Task | Status |
|---|------|--------|
| 4.1 | Spring Boot edge profile (embedded DB, single-writer) | ⏳ |
| 4.2 | SQLCipher-based local store | ⏳ |
| 4.3 | Event envelope (hash-chained, device-signed) | ⏳ |
| 4.4 | Sync gateway module (`syn_`) — push/pull/lease endpoints | ⏳ |
| 4.5 | Conflict resolution engine | ⏳ |
| 4.6 | BLN installer packaging & auto-update | ⏳ |

---

## P5–P10 (queued, details added as phases begin)

---

## Decisions Log

| ADR | Decision | Resolved? |
|-----|----------|-----------|
| 001 | Modular monolith | ✅ Accepted |
| 002 | Pooled + silo tenancy with RLS | ✅ Accepted |
| 003 | PostgreSQL as SoR | ✅ Accepted |
| 004 | BLN + thin terminals | ✅ Accepted |
| 005 | Append-only hash-chained sync | ✅ Accepted |
| 006 | Next.js: terminal static export | ✅ Accepted |
| 007 | Keycloak OIDC, two realms | ✅ Accepted |
| 008 | Transactional outbox, no broker | ✅ Accepted |
| 009 | Money as bigint minor units; UUIDv7 | ✅ Accepted |
| 010 | Stock = sum of immutable movements | ✅ Accepted |
| 011 | Local DB engine (SQLite vs PG) | 🔲 P0 spike |
| 012 | Service-worker library | 🔲 Open |
| 013 | LAN HTTPS / cert authority | 🔲 P0 spike |
| 014 | Flyway vs Liquibase | 🔲 Proposed: Flyway |
| 015 | Hosting region | 🔲 Legal |
| 016 | Job scheduler (db-scheduler / JobRunr) | 🔲 Open |
| 017 | Stock valuation (WAVG vs FIFO) | 🔲 Product |

---

## Build Log

| Timestamp | Event |
|-----------|-------|
| 2026-11-18 18:48 | Project scoped from TDD; status.md created |
| 2026-11-18 18:48 | Swarm launched — P0 scaffold workers started |
| 2026-11-19 14:28 | P2 complete — Multitenancy + IAM + Security layer: 48 Java files + 1 SQL migration |
| 2026-11-19 16:45 | P0 complete — Infrastructure, contracts, shared-kernel, app configuration, ADRs, QUICKSTART guide |

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Done |
| 🔄 | In progress |
| ⏳ | Queued / not started |
| ❌ | Blocked |
| 🔲 | Decision pending |
