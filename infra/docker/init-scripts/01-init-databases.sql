-- PharmaPOS Database Initialization Script
-- This script creates the required databases, users, and security policies

-- =========================================================================
-- 1. Create Databases
-- =========================================================================

-- Pool database for multi-tenant data with Row-Level Security
CREATE DATABASE pharmapos_pool
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Platform database for control plane (tenant directory, subscriptions)
CREATE DATABASE pharmapos_platform
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Keycloak database
CREATE DATABASE keycloak
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- =========================================================================
-- 2. Create Application Roles
-- =========================================================================

-- Read-Write Application Role (no BYPASSRLS, not table owner - enforces RLS)
CREATE ROLE app_rw WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'app_rw_secure_password';

COMMENT ON ROLE app_rw IS 'Application read-write role - enforces Row-Level Security';

-- Read-Only Role for Reports (access to replicas)
CREATE ROLE app_ro_reports WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'app_ro_secure_password';

COMMENT ON ROLE app_ro_reports IS 'Read-only role for reporting and analytics';

-- Migration Role (owns tables, used only by CI/CD pipeline)
CREATE ROLE migrator WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT 5
    PASSWORD 'migrator_secure_password';

COMMENT ON ROLE migrator IS 'Migration role - owns tables, used only by deployment pipeline';

-- Platform Admin Role (control plane only)
CREATE ROLE platform_admin WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT 10
    PASSWORD 'platform_admin_secure_password';

COMMENT ON ROLE platform_admin IS 'Platform administration role for tenant management';

-- Keycloak Database User
CREATE ROLE keycloak WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'keycloak_secure_password';

-- =========================================================================
-- 3. Grant Database Access
-- =========================================================================

-- Pool Database Grants
GRANT CONNECT ON DATABASE pharmapos_pool TO app_rw;
GRANT CONNECT ON DATABASE pharmapos_pool TO app_ro_reports;
GRANT CONNECT ON DATABASE pharmapos_pool TO migrator;

-- Platform Database Grants
GRANT CONNECT ON DATABASE pharmapos_platform TO platform_admin;
GRANT CONNECT ON DATABASE pharmapos_platform TO migrator;
GRANT CONNECT ON DATABASE pharmapos_platform TO app_rw;

-- Keycloak Database Grants
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- =========================================================================
-- 4. Configure Pool Database
-- =========================================================================

\c pharmapos_pool

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- Trigram search for product names
CREATE EXTENSION IF NOT EXISTS "btree_gin"; -- Compound indexes
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements"; -- Query performance monitoring

-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO app_rw;
GRANT USAGE ON SCHEMA public TO app_ro_reports;
GRANT ALL ON SCHEMA public TO migrator;

-- Default privileges for future objects created by migrator
ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
GRANT SELECT ON TABLES TO app_ro_reports;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO app_rw;

-- =========================================================================
-- 5. Configure Platform Database
-- =========================================================================

\c pharmapos_platform

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO platform_admin;
GRANT USAGE ON SCHEMA public TO app_rw;
GRANT ALL ON SCHEMA public TO migrator;

-- Default privileges
ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO platform_admin;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
GRANT SELECT ON TABLES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO platform_admin;

-- =========================================================================
-- 6. Security Configuration
-- =========================================================================

\c pharmapos_pool

-- Set secure configuration parameters
ALTER DATABASE pharmapos_pool SET log_statement = 'mod';
ALTER DATABASE pharmapos_pool SET log_duration = on;
ALTER DATABASE pharmapos_pool SET log_min_duration_statement = 1000; -- Log queries > 1s

\c pharmapos_platform

ALTER DATABASE pharmapos_platform SET log_statement = 'mod';
ALTER DATABASE pharmapos_platform SET log_duration = on;
ALTER DATABASE pharmapos_platform SET log_min_duration_statement = 1000;

-- =========================================================================
-- Initialization Complete
-- =========================================================================

\c postgres

SELECT 'Database initialization completed successfully.' AS status;
SELECT datname, pg_encoding_to_char(encoding) AS encoding, datcollate, datctype 
FROM pg_database 
WHERE datname IN ('pharmapos_pool', 'pharmapos_platform', 'keycloak');
