-- ============================================================================
-- V001__create_iam_tables.sql
-- IAM module: users, roles, role assignments, devices, idempotency
-- Migration run by the `migrator` role (owns tables).
-- Application role (app_rw) uses RLS; it has no BYPASSRLS and no DDL rights.
-- ============================================================================

-- ── iam_user ─────────────────────────────────────────────────────────────────
CREATE TABLE iam_user (
    id              uuid        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       uuid        NOT NULL,
    keycloak_id     text        NOT NULL,
    username        text        NOT NULL,
    email           text        NOT NULL,
    status          text        NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED')),
    mfa_enabled     boolean     NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_user PRIMARY KEY (id)
);

-- Fast lookup: token sub → internal user (TDD §4.4 "Index on (tenant_id, keycloak_id)")
CREATE UNIQUE INDEX idx_iam_user_tenant_keycloak ON iam_user (tenant_id, keycloak_id);
CREATE        INDEX idx_iam_user_tenant_email    ON iam_user (tenant_id, email);
CREATE        INDEX idx_iam_user_status          ON iam_user (tenant_id, status)
              WHERE status != 'ACTIVE';

-- RLS: tenant isolation
ALTER TABLE iam_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_user FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON iam_user
    USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- ── iam_role ──────────────────────────────────────────────────────────────────
CREATE TABLE iam_role (
    id          uuid    NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   uuid    NOT NULL,
    name        text    NOT NULL,
    version     bigint  NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_role PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_iam_role_tenant_name ON iam_role (tenant_id, name);

ALTER TABLE iam_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_role FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON iam_role
    USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- ── iam_role_permission (element collection for Role.permissions) ─────────────
CREATE TABLE iam_role_permission (
    role_id    uuid NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
    permission text NOT NULL,
    CONSTRAINT pk_iam_role_permission PRIMARY KEY (role_id, permission)
);

CREATE INDEX idx_iam_rp_role ON iam_role_permission (role_id);

-- ── iam_role_assignment ───────────────────────────────────────────────────────
CREATE TABLE iam_role_assignment (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL,
    role_id     uuid        NOT NULL REFERENCES iam_role(id),
    branch_id   uuid,                   -- NULL = tenant-wide access
    assigned_at timestamptz NOT NULL DEFAULT now(),
    assigned_by uuid        NOT NULL,
    CONSTRAINT pk_iam_role_assignment PRIMARY KEY (id),
    CONSTRAINT uq_iam_ra_user_role_branch UNIQUE (user_id, role_id, branch_id)
);

CREATE INDEX idx_iam_ra_user   ON iam_role_assignment (user_id);
CREATE INDEX idx_iam_ra_role   ON iam_role_assignment (role_id);
CREATE INDEX idx_iam_ra_branch ON iam_role_assignment (branch_id) WHERE branch_id IS NOT NULL;

-- Role assignments are not tenant-RLS-filtered directly (they join to iam_user).
-- Access is controlled via the user_id FK — the user is always in the same tenant.

-- ── iam_device ────────────────────────────────────────────────────────────────
CREATE TABLE iam_device (
    id               uuid        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id        uuid        NOT NULL,
    branch_id        uuid        NOT NULL,
    name             text        NOT NULL,
    public_key_pem   text        NOT NULL,
    certificate_pem  text        NOT NULL,
    status           text        NOT NULL DEFAULT 'ACTIVE'
                                 CHECK (status IN ('ACTIVE', 'REVOKED')),
    enrolled_at      timestamptz NOT NULL DEFAULT now(),
    revoked_at       timestamptz,
    revoked_by       uuid,
    lease_expires_at timestamptz,
    version          bigint      NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_device PRIMARY KEY (id)
);

CREATE INDEX idx_iam_dev_tenant_branch ON iam_device (tenant_id, branch_id);
CREATE INDEX idx_iam_dev_status        ON iam_device (status) WHERE status = 'ACTIVE';

ALTER TABLE iam_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_device FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON iam_device
    USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- ── sys_idempotency ───────────────────────────────────────────────────────────
-- Shared infrastructure table — prefix sys_ signals kernel ownership.
-- Insert-only: the application role has no UPDATE or DELETE privilege (TDD §9.3).
CREATE TABLE sys_idempotency (
    id                   uuid        NOT NULL DEFAULT gen_random_uuid(),
    idempotency_key      text        NOT NULL,
    tenant_id            uuid        NOT NULL,
    request_fingerprint  text        NOT NULL,
    response_status      integer     NOT NULL,
    response_body        text,
    response_content_type text,
    created_at           timestamptz NOT NULL DEFAULT now(),
    expires_at           timestamptz NOT NULL,
    CONSTRAINT pk_sys_idempotency PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_idmp_key_tenant ON sys_idempotency (idempotency_key, tenant_id);
CREATE        INDEX idx_idmp_expires    ON sys_idempotency (expires_at);

ALTER TABLE sys_idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE sys_idempotency FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sys_idempotency
    USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- Insert-only: revoke UPDATE and DELETE from the application role.
-- (The migrator role owns the table and retains full access for the cleanup job.)
REVOKE UPDATE, DELETE ON sys_idempotency FROM app_rw;

-- ── sys_pending_approval ──────────────────────────────────────────────────────
-- Cross-module approval queue used by DisableUserUseCase to flag reassignments.
CREATE TABLE sys_pending_approval (
    id                        uuid        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id                 uuid        NOT NULL,
    entity_type               text        NOT NULL,  -- e.g. 'prc_purchase_order'
    entity_id                 uuid        NOT NULL,
    assignee_id               uuid,                  -- NULL = unassigned
    flagged_for_reassignment  boolean     NOT NULL DEFAULT false,
    status                    text        NOT NULL DEFAULT 'PENDING'
                                          CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    created_at                timestamptz NOT NULL DEFAULT now(),
    updated_at                timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_sys_pending_approval PRIMARY KEY (id)
);

CREATE INDEX idx_spa_tenant_status    ON sys_pending_approval (tenant_id, status);
CREATE INDEX idx_spa_assignee         ON sys_pending_approval (assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_spa_flagged          ON sys_pending_approval (flagged_for_reassignment)
             WHERE flagged_for_reassignment = true;

ALTER TABLE sys_pending_approval ENABLE ROW LEVEL SECURITY;
ALTER TABLE sys_pending_approval FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sys_pending_approval
    USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- ── Grants for application roles ──────────────────────────────────────────────
-- app_rw: DML on all IAM tables (RLS enforces tenant scoping).
-- app_ro_reports: SELECT on all IAM tables (read replica, also RLS-filtered).
-- migrator: already owner; no extra GRANT needed.

GRANT SELECT, INSERT, UPDATE, DELETE ON iam_user            TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON iam_role            TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON iam_role_permission TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON iam_role_assignment TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON iam_device          TO app_rw;
GRANT SELECT, INSERT               ON sys_idempotency       TO app_rw;  -- insert-only
GRANT SELECT, INSERT, UPDATE        ON sys_pending_approval  TO app_rw;

GRANT SELECT ON iam_user, iam_role, iam_role_permission,
               iam_role_assignment, iam_device, sys_idempotency,
               sys_pending_approval                          TO app_ro_reports;
