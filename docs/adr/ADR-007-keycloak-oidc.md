# ADR-007 — Keycloak-Class OIDC Provider, Separate Realms for Tenants and Platform Staff

## Status

Accepted (Plan §6.2)

## Context

PharmaPOS must authenticate several actor types — cashiers, pharmacists, branch managers, owners, accountants, platform administrators, and support engineers — with varying levels of trust and MFA requirements. The system must support:

- Standard OIDC/OAuth 2.0 flows for web and mobile applications
- Multi-factor authentication (WebAuthn/passkeys, TOTP, SMS fallback)
- Per-branch role assignments with value limits (e.g., refund caps)
- Device enrolment and revocation
- Separation between tenant users and platform staff (control-plane isolation)
- Cashier PIN access on enrolled devices with idle-lock
- Break-glass support access that is tenant-approved, time-limited, and audited

Building authentication from scratch is explicitly rejected by the Plan.

## Decision

Use **Keycloak** (or a comparable OIDC-compliant identity provider) with **two separate realms**:

| Realm | Users | Purpose |
|---|---|---|
| `tenant` | Cashiers, pharmacists, branch managers, owners, accountants | Business user authentication; federated per tenant |
| `platform` | Platform administrators, support engineers | Control-plane operations; separate network path |

### Authentication flows

| Scenario | Flow |
|---|---|
| Back-office login | OIDC authorisation-code flow with PKCE |
| Terminal login | Username + PIN/password on an enrolled device; idle-lock after 5 minutes with PIN unlock |
| Platform console | OIDC authorisation-code flow with PKCE; mandatory MFA for all platform staff |
| Supervisor override | Supervisor credential (PIN offline, standard approval online) for high-risk actions |

### Token design

| Token | Lifetime | Storage |
|---|---|---|
| Access token | 10–15 minutes | In memory (never localStorage) |
| Refresh token | Rotating; bound to the device | Secure, HttpOnly cookie or in-memory |
| Absolute session | 12 hours | Server-side revocation list |

### MFA requirements

Mandatory for: owners, administrators, accountants, approving pharmacists, all platform staff.
Methods: WebAuthn/passkeys (preferred) → TOTP → SMS (fallback; tenant can disable).

### Password policy

Minimum 12 characters, breached-password check (HaveIBeenPwned k-anonymity), no forced periodic rotation, Argon2id with per-user salt.

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| Hand-built authentication | Explicitly rejected by the Plan; too risky for a regulated system handling clinical and financial data |
| Auth0 / Okta | Viable but adds vendor lock-in and per-user pricing that may not suit the target market's economics; Keycloak is self-hosted and open source |
| Single realm with organizational units | Does not achieve the control-plane isolation required by the security design; platform staff and tenant users must have no shared session surface |
| Firebase Auth | Insufficient fine-grained role management; no self-hosted option; limited MFA options |

## Consequences

**Positive:**
- No custom password storage, session management, or token issuance in the application.
- Two-realm design enforces control-plane isolation at the identity layer.
- Keycloak's built-in support for OIDC, SAML, MFA, user federation, and social login covers current and future requirements.
- Access tokens carry tenant, branch, and role claims that the application validates on every request.
- Cashier offline authentication uses local credential caches (Argon2id hashes) with lockout and delay.

**Negative / Risks:**
- Keycloak is a Java application with its own operational overhead (HA, upgrades, theme customisation).
- The offline BLN must cache credentials and validate PINs locally; the credential cache must be encrypted and must expire.
- Device revocation is eventually consistent: a revoked device can still operate until the BLN syncs or the lease expires.
- Break-glass support access requires custom Keycloak extensions or application-layer grants on top of the standard OIDC flow.

## References

- TDD §2.1, §9.1–9.2
- Plan §6.2, §3.2–3.5
