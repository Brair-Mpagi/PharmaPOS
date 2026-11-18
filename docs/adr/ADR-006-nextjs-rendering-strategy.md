# ADR-006 — Next.js Rendering Strategy: Terminal Static Export, Back-Office SSR, Spring Boot Holds All Business Rules

## Status

Accepted (Plan §5.7)

## Context

PharmaPOS has three frontend applications with very different runtime constraints:

1. **Terminal app** — Must work offline on the branch LAN, served by the BLN; runs on counter PCs and phones. No internet access is guaranteed. Camera barcode scanning requires HTTPS.

2. **Back-office app** — Online-only; used by owners, managers, and accountants for dashboards, reports, procurement, and administration. Can benefit from server-side rendering for SEO (not critical), faster first paint, and secure cookie-based sessions.

3. **Platform console** — Online-only; used by platform administrators and support engineers. Separate deployment and identity realm (control-plane isolation).

All three applications consume the same REST API (defined by the OpenAPI contract in `contracts/`). The API client is generated from this contract so frontend types cannot drift from the backend.

## Decision

Use **Next.js** (App Router, TypeScript, React) for all three applications, with different rendering modes:

| App | Rendering | Build Output | Served From |
|---|---|---|---|
| Terminal | Static export (`output: 'export'`), client-rendered PWA with service worker | Static HTML/JS/CSS bundle | BLN on the LAN; same bundle from CDN when online |
| Back-office | Server components and SSR | Node.js server | Cloud, behind the edge |
| Platform console | Server components and SSR | Node.js server | Cloud, separate network path |

**Critical rule:** Spring Boot is the **only** place with business rules, authorisation, and database access. The Next.js server layer handles session cookies, OIDC redirects, and rendering — it holds no database credentials and no business logic.

The three apps share a `web/packages/` directory containing:
- Generated API client (from the OpenAPI contract)
- UI component kit
- Zod validation schemas (aligned to the API contract)

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Single Next.js app with conditional rendering | The terminal's static-export constraint (`output: 'export'`) is incompatible with SSR used by the back-office; mixing them in one app creates build complexity and security risks |
| Separate React SPA (Vite) for the terminal | Viable, but Next.js's static export mode covers the requirement; using the same framework across all three apps reduces the learning curve and allows shared packages |
| Angular or Vue | The team has React experience; Next.js has the strongest offline/PWA ecosystem in the React world |
| Server-side rendering for the terminal | Impossible — the BLN does not run a Node.js server; the terminal must be a static bundle |

## Consequences

**Positive:**
- The terminal works fully offline; no server-side rendering dependency.
- One framework, one language (TypeScript), one component library across all three apps.
- The generated API client guarantees type safety end-to-end.
- The back-office benefits from server components for data fetching without exposing API tokens to the browser.
- The platform console is isolated: separate deployment, separate identity realm, separate network path.

**Negative / Risks:**
- Next.js has had security advisories (middleware bypass, SSRF); the team must keep it patched and check advisories at kickoff and on every release. Never rely on Next.js middleware or route guards for authorisation.
- The terminal's static export means no server components and no API routes in the terminal app. All data fetching is client-side via TanStack Query.
- Three separate build and deployment pipelines (though they share packages and the generated client).
- The terminal's JavaScript bundle must stay within the 250 KB gzipped performance budget (proposed).

## References

- TDD §2.1, §10.1–10.5
- Plan §5.7
