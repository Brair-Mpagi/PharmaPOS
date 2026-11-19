-- =========================================================================
-- PharmaPOS Identity Module - Initial Schema
-- Module prefix: iam_
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. Users Table
-- -------------------------------------------------------------------------
CREATE TABLE iam_user (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    username            VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    phone_number        VARCHAR(50),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    external_auth_id    VARCHAR(255), -- Keycloak user ID
    last_login_at       TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_at          TIMESTAMP WITH TIME ZONE,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT iam_user_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED')),
    CONSTRAINT iam_user_tenant_username_unique UNIQUE (tenant_id, username),
    CONSTRAINT iam_user_tenant_email_unique UNIQUE (tenant_id, email)
);

CREATE INDEX idx_iam_user_tenant ON iam_user(tenant_id);
CREATE INDEX idx_iam_user_status ON iam_user(tenant_id, status);
CREATE INDEX idx_iam_user_external_auth ON iam_user(external_auth_id);

COMMENT ON TABLE iam_user IS 'System users per tenant';
COMMENT ON COLUMN iam_user.external_auth_id IS 'Keycloak user identifier for SSO';

-- Row-Level Security
ALTER TABLE iam_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_user FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_user_tenant_isolation ON iam_user
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 2. Roles Table
-- -------------------------------------------------------------------------
CREATE TABLE iam_role (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    is_system_role      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_at          TIMESTAMP WITH TIME ZONE,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT iam_role_tenant_name_unique UNIQUE (tenant_id, name)
);

CREATE INDEX idx_iam_role_tenant ON iam_role(tenant_id);

COMMENT ON TABLE iam_role IS 'User roles per tenant';
COMMENT ON COLUMN iam_role.is_system_role IS 'System roles cannot be deleted';

-- Row-Level Security
ALTER TABLE iam_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_role FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_role_tenant_isolation ON iam_role
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 3. Permissions Table
-- -------------------------------------------------------------------------
CREATE TABLE iam_permission (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    code                VARCHAR(100) NOT NULL,
    resource            VARCHAR(100) NOT NULL,
    action              VARCHAR(50) NOT NULL,
    description         TEXT,
    is_system_permission BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT iam_permission_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE INDEX idx_iam_permission_tenant ON iam_permission(tenant_id);
CREATE INDEX idx_iam_permission_resource ON iam_permission(tenant_id, resource);

COMMENT ON TABLE iam_permission IS 'Fine-grained permissions per tenant';

-- Row-Level Security
ALTER TABLE iam_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_permission FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_permission_tenant_isolation ON iam_permission
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 4. Role-Permission Assignments
-- -------------------------------------------------------------------------
CREATE TABLE iam_role_permission (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    role_id             UUID NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
    permission_id       UUID NOT NULL REFERENCES iam_permission(id) ON DELETE CASCADE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    
    CONSTRAINT iam_role_permission_unique UNIQUE (tenant_id, role_id, permission_id)
);

CREATE INDEX idx_iam_role_permission_role ON iam_role_permission(role_id);
CREATE INDEX idx_iam_role_permission_permission ON iam_role_permission(permission_id);

COMMENT ON TABLE iam_role_permission IS 'Many-to-many: roles to permissions';

-- Row-Level Security
ALTER TABLE iam_role_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_role_permission FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_role_permission_tenant_isolation ON iam_role_permission
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 5. User-Role Assignments
-- -------------------------------------------------------------------------
CREATE TABLE iam_user_role (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    user_id             UUID NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
    role_id             UUID NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
    branch_id           UUID, -- Nullable: role applies to specific branch only
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    
    CONSTRAINT iam_user_role_unique UNIQUE (tenant_id, user_id, role_id, branch_id)
);

CREATE INDEX idx_iam_user_role_user ON iam_user_role(user_id);
CREATE INDEX idx_iam_user_role_role ON iam_user_role(role_id);
CREATE INDEX idx_iam_user_role_branch ON iam_user_role(branch_id) WHERE branch_id IS NOT NULL;

COMMENT ON TABLE iam_user_role IS 'Many-to-many: users to roles with optional branch scope';

-- Row-Level Security
ALTER TABLE iam_user_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_user_role FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_user_role_tenant_isolation ON iam_user_role
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 6. Devices Table (Branch Local Nodes and Terminals)
-- -------------------------------------------------------------------------
CREATE TABLE iam_device (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    branch_id           UUID NOT NULL,
    device_name         VARCHAR(255) NOT NULL,
    device_type         VARCHAR(20) NOT NULL,
    certificate_fingerprint VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_seen_at        TIMESTAMP WITH TIME ZONE,
    enrolled_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_at          TIMESTAMP WITH TIME ZONE,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT iam_device_type_check CHECK (device_type IN ('BLN', 'TERMINAL', 'MOBILE')),
    CONSTRAINT iam_device_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT iam_device_tenant_name_unique UNIQUE (tenant_id, branch_id, device_name)
);

CREATE INDEX idx_iam_device_tenant_branch ON iam_device(tenant_id, branch_id);
CREATE INDEX idx_iam_device_status ON iam_device(tenant_id, status);
CREATE INDEX idx_iam_device_certificate ON iam_device(certificate_fingerprint) WHERE certificate_fingerprint IS NOT NULL;

COMMENT ON TABLE iam_device IS 'Registered devices per branch';
COMMENT ON COLUMN iam_device.certificate_fingerprint IS 'Device certificate SHA-256 fingerprint for mTLS';

-- Row-Level Security
ALTER TABLE iam_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_device FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_device_tenant_isolation ON iam_device
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- -------------------------------------------------------------------------
-- 7. User Sessions Table
-- -------------------------------------------------------------------------
CREATE TABLE iam_session (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    user_id             UUID NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
    device_id           UUID REFERENCES iam_device(id) ON DELETE SET NULL,
    token_hash          VARCHAR(255) NOT NULL,
    ip_address          VARCHAR(50),
    user_agent          VARCHAR(500),
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ended_at            TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT iam_session_token_unique UNIQUE (token_hash)
);

CREATE INDEX idx_iam_session_user ON iam_session(user_id);
CREATE INDEX idx_iam_session_device ON iam_session(device_id) WHERE device_id IS NOT NULL;
CREATE INDEX idx_iam_session_expires ON iam_session(expires_at) WHERE ended_at IS NULL;

COMMENT ON TABLE iam_session IS 'Active user sessions for audit and revocation';

-- Row-Level Security
ALTER TABLE iam_session ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_session FORCE ROW LEVEL SECURITY;

CREATE POLICY iam_session_tenant_isolation ON iam_session
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- =========================================================================
-- Seed System Permissions (examples)
-- =========================================================================

-- Note: Actual permission seeding will be done via application code
-- This is just a demonstration of the structure

COMMENT ON SCHEMA public IS 'PharmaPOS Identity Schema - V001 Initial';
