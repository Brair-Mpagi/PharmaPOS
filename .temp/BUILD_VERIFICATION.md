# PharmaPOS Build Verification Guide

This document provides step-by-step verification that the PharmaPOS system is correctly built and operational.

## ✅ Verification Checklist

### Phase 0: Infrastructure Components

- [ ] **Gradle Wrapper Exists**
  ```bash
  cd backend
  ls -la gradlew
  # Should show executable wrapper script
  ```

- [ ] **Docker Compose Configuration**
  ```bash
  cd infra/docker
  ls -la docker-compose.yml .env
  # Should show compose file and environment config
  ```

- [ ] **OpenAPI Contract Exists**
  ```bash
  ls -la contracts/openapi/pharmapos-api.yaml
  # Should show API specification file
  ```

- [ ] **ADR Documentation**
  ```bash
  ls -la docs/adr/
  # Should show ADR-001, ADR-002, ADR-014, and README.md
  ```

---

### Phase 1: Infrastructure Services

Start all infrastructure services:
```bash
cd infra/docker
docker compose up -d
```

#### 1.1 PostgreSQL Health Check
```bash
docker exec pharmapos-postgres pg_isready
# Expected: pharmapos-postgres:5432 - accepting connections
```

Verify databases:
```bash
docker exec -it pharmapos-postgres psql -U postgres -c "\l"
# Should list: pharmapos_pool, pharmapos_platform, keycloak
```

- [ ] PostgreSQL is running and healthy
- [ ] All three databases exist

#### 1.2 Keycloak Health Check
```bash
curl -f http://localhost:8080/health/ready
# Expected: HTTP 200 OK
```

Access admin console:
- URL: `http://localhost:8080`
- Credentials: admin/admin

- [ ] Keycloak is accessible
- [ ] Can login to admin console

#### 1.3 Redis Health Check
```bash
docker exec pharmapos-redis redis-cli ping
# Expected: PONG
```

- [ ] Redis is responding

#### 1.4 MinIO Health Check
```bash
curl -f http://localhost:9000/minio/health/live
# Expected: HTTP 200 OK
```

Access console:
- URL: `http://localhost:9001`
- Credentials: minioadmin/minioadmin123

- [ ] MinIO is accessible
- [ ] Can login to console

#### 1.5 MailHog Health Check
```bash
curl -f http://localhost:8025
# Expected: HTTP 200 OK
```

- [ ] MailHog web UI is accessible

---

### Phase 2: Database Schema

Run Flyway migrations:
```bash
cd backend
./gradlew :app-cloud:flywayMigrate
```

Expected output:
```
Successfully applied 1 migration to schema "public"
```

#### 2.1 Verify Identity Module Tables
```bash
docker exec -it pharmapos-postgres psql -U postgres -d pharmapos_pool -c "\dt iam_*"
```

Should show:
- `iam_user`
- `iam_role`
- `iam_permission`
- `iam_role_permission`
- `iam_user_role`
- `iam_device`
- `iam_session`

- [ ] All identity tables exist

#### 2.2 Verify Row-Level Security
```bash
docker exec -it pharmapos-postgres psql -U postgres -d pharmapos_pool -c "
SELECT tablename, policyname 
FROM pg_policies 
WHERE schemaname = 'public' AND tablename LIKE 'iam_%';"
```

Should show RLS policies for all `iam_*` tables.

- [ ] RLS policies are created

#### 2.3 Verify Extensions
```bash
docker exec -it pharmapos-postgres psql -U postgres -d pharmapos_pool -c "\dx"
```

Should include:
- `uuid-ossp`
- `pg_trgm`
- `btree_gin`

- [ ] PostgreSQL extensions are installed

---

### Phase 3: Backend Application

Start the backend:
```bash
cd backend
./gradlew :app-cloud:bootRun --args='--spring.profiles.active=local'
```

Wait for startup (typically 10-30 seconds).

#### 3.1 Health Check
```bash
curl http://localhost:8081/api/v1/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2026-11-19T...",
  "application": "pharmapos-cloud",
  "version": "0.1.0-SNAPSHOT"
}
```

- [ ] API responds to health check

#### 3.2 Actuator Health
```bash
curl http://localhost:8081/actuator/health
```

Expected:
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

- [ ] Spring Boot actuator is working

#### 3.3 Database Connectivity
```bash
curl http://localhost:8081/actuator/health/db
```

Expected:
```json
{
  "status": "UP"
}
```

- [ ] Application can connect to database

#### 3.4 Swagger UI
Open browser: `http://localhost:8081/swagger-ui.html`

Should show:
- PharmaPOS Enterprise API documentation
- Catalogue endpoints
- Inventory endpoints
- Sales endpoints

- [ ] Swagger UI is accessible
- [ ] API endpoints are documented

---

### Phase 4: Shared Kernel Module

#### 4.1 Money Value Object
```bash
cd backend/modules/shared-kernel
ls -la src/main/java/com/pharmapos/shared/valueobject/Money.java
```

- [ ] Money.java exists and compiles

#### 4.2 Base Entity Classes
```bash
ls -la src/main/java/com/pharmapos/shared/domain/
```

Should show:
- `BaseEntity.java`
- `TenantEntity.java`
- `BranchEntity.java`

- [ ] Base entity classes exist

#### 4.3 Tenant Context
```bash
ls -la src/main/java/com/pharmapos/shared/context/
```

Should show:
- `TenantContext.java`
- `TenantContextHolder.java`

- [ ] Tenant context classes exist

---

### Phase 5: Build and Test

#### 5.1 Compile All Modules
```bash
cd backend
./gradlew clean build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] All modules compile without errors

#### 5.2 Run Unit Tests
```bash
./gradlew test
```

Expected: All tests pass (or skip if none exist yet)

- [ ] Tests execute successfully

#### 5.3 Verify Module Boundaries
```bash
./gradlew verifyModuleBoundaries
```

This runs Spring Modulith structural tests.

- [ ] Module boundaries are enforced

---

### Phase 6: Frontend Applications

#### 6.1 Terminal App Structure
```bash
cd web/apps/terminal
ls -la
```

Should show basic Next.js structure.

- [ ] Terminal app directory exists

#### 6.2 Install Dependencies
```bash
cd web
npm install
```

Expected: Dependencies installed successfully

- [ ] NPM dependencies install without errors

---

## 🔧 Troubleshooting

### Database Connection Issues

**Problem:** Application cannot connect to PostgreSQL

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep pharmapos-postgres

# Check PostgreSQL logs
docker logs pharmapos-postgres

# Restart PostgreSQL
docker compose restart postgres
```

### Port Conflicts

**Problem:** Port already in use (e.g., 8081, 5432)

**Solution:**
```bash
# Find process using the port
lsof -i :8081

# Kill the process
kill -9 <PID>

# Or change port in application.yml
```

### Gradle Build Failures

**Problem:** Gradle build fails with dependency errors

**Solution:**
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches/

# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

### Docker Compose Failures

**Problem:** Services fail to start

**Solution:**
```bash
# Stop all services
docker compose down

# Remove volumes (CAUTION: Deletes data)
docker compose down -v

# Start fresh
docker compose up -d
```

---

## 📊 Current Build Status

Based on this verification, the project includes:

### ✅ Completed
- [x] Gradle multi-project build system
- [x] Docker Compose infrastructure
- [x] PostgreSQL with multi-tenancy setup
- [x] Flyway migration framework
- [x] Identity module database schema
- [x] Shared kernel (Money, Quantity, base entities)
- [x] Tenant context and security infrastructure
- [x] Spring Boot application configuration
- [x] Health check endpoints
- [x] OpenAPI specification
- [x] ADR documentation
- [x] Quick Start guide

### 🔄 In Progress
- [ ] Domain module implementations (Catalogue, Inventory, Sales)
- [ ] Frontend application implementations
- [ ] Keycloak realm configuration
- [ ] Integration tests
- [ ] CI/CD pipeline

### ⏳ Not Started
- [ ] Branch Local Node (BLN) implementation
- [ ] Sync gateway
- [ ] Clinical module
- [ ] Procurement module
- [ ] Finance module
- [ ] Production deployment configuration

---

## 🎯 Next Steps

1. **For Backend Development:**
   - Implement Catalogue module domain model and repositories
   - Create REST controllers with Spring Security
   - Add comprehensive unit and integration tests

2. **For Frontend Development:**
   - Set up Next.js applications with proper routing
   - Create UI components and forms
   - Integrate with backend API

3. **For DevOps:**
   - Configure Keycloak realms and clients
   - Set up CI/CD pipeline
   - Create production deployment scripts

---

## 📝 Sign-Off

Use this checklist during code reviews and deployment:

**Developer:** _________________  **Date:** __________

**Reviewer:** _________________  **Date:** __________

**Status:** [ ] Verified [ ] Issues Found

**Notes:**
