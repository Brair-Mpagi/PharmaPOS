# PharmaPOS Quick Start Guide

This guide will get you from zero to a running PharmaPOS system in under 15 minutes.

## Prerequisites

Ensure you have installed:
- **Docker Desktop 4.x** or **Docker Engine 24+** with Compose v2
- **Java 25 JDK** ([Download from Oracle](https://www.oracle.com/java/technologies/downloads/#java25) or use SDKMAN)
- **Node.js 22 LTS** ([Download from nodejs.org](https://nodejs.org/))
- **Git**

Verify installations:
```bash
docker --version          # Should be 24.0+
docker compose version    # Should be v2.x
java -version             # Should show Java 25
node --version            # Should show v22.x
```

---

## Step 1: Clone the Repository

```bash
git clone <repository-url>
cd PharmaPOS
```

---

## Step 2: Start Infrastructure Services

Start PostgreSQL, Keycloak, Redis, MinIO, and MailHog:

```bash
cd infra/docker
docker compose up -d
```

Wait for all services to be healthy (about 60 seconds):
```bash
docker compose ps
```

All services should show "healthy" status.

**Service Endpoints:**
- PostgreSQL: `localhost:5432`
- Keycloak Admin: `http://localhost:8080` (admin/admin)
- Redis: `localhost:6379`
- MinIO Console: `http://localhost:9001` (minioadmin/minioadmin123)
- MailHog UI: `http://localhost:8025`

---

## Step 3: Run Database Migrations

```bash
cd ../../backend
./gradlew :app-cloud:flywayMigrate
```

This creates:
- Database schemas for all modules
- Row-Level Security policies
- Initial tables

Verify migration:
```bash
docker exec -it pharmapos-postgres psql -U postgres -d pharmapos_pool -c "\dt"
```

You should see tables like `iam_user`, `iam_role`, `iam_permission`, etc.

---

## Step 4: Start the Backend API

```bash
./gradlew :app-cloud:bootRun --args='--spring.profiles.active=local'
```

The API will start on `http://localhost:8081`.

**Verify it's running:**
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

**API Documentation:**
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- Actuator: `http://localhost:8081/actuator/health`

---

## Step 5: Start the Frontend (Terminal App)

Open a new terminal:

```bash
cd web/apps/terminal
npm install
npm run dev
```

The terminal app will start on `http://localhost:3000`.

**Optional: Start other frontend apps**
```bash
# Back-office (in another terminal)
cd web/apps/backoffice
npm install
npm run dev  # Runs on http://localhost:3001

# Platform console (in another terminal)
cd web/apps/platform
npm install
npm run dev  # Runs on http://localhost:3002
```

---

## Step 6: Configure Keycloak (First Time Only)

1. Open Keycloak Admin Console: `http://localhost:8080`
2. Login with `admin` / `admin`
3. Create a new realm called `tenant`
4. Create a client for the API:
   - Client ID: `pharmapos-api`
   - Client Protocol: `openid-connect`
   - Access Type: `bearer-only`
5. Create a test user in the `tenant` realm
6. Assign roles to the user

**Detailed Keycloak setup instructions:** See `docs/keycloak-setup.md` (to be created)

---

## Step 7: Test the System

### Test Health Endpoint (No Auth Required)
```bash
curl http://localhost:8081/api/v1/health
```

### Get Access Token from Keycloak
```bash
curl -X POST http://localhost:8080/realms/tenant/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=testpassword" \
  -d "grant_type=password" \
  -d "client_id=pharmapos-api"
```

### Call Protected Endpoint
```bash
TOKEN="<access_token_from_previous_step>"

curl http://localhost:8081/api/v1/catalogue/items \
  -H "Authorization: Bearer $TOKEN"
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  Frontend Layer (Next.js)                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Terminal   │  │  Back-office │  │   Platform   │     │
│  │   (PWA)      │  │   (SSR)      │  │   Console    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS/REST
┌────────────────────────▼────────────────────────────────────┐
│  API Gateway + Security Layer                               │
│  - JWT Validation                                           │
│  - Tenant Context Extraction                                │
│  - Rate Limiting                                            │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│  Spring Boot Application (Modular Monolith)                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Modules (Bounded Contexts)                         │   │
│  │  ┌──────┐ ┌─────────┐ ┌──────┐ ┌────────┐         │   │
│  │  │ IAM  │ │Catalogue│ │Invent│ │ Sales  │ ...     │   │
│  │  └──────┘ └─────────┘ └──────┘ └────────┘         │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                   │
│                 Transactional Outbox                        │
└────────────────────────┬─────────────────────────────────┬──┘
                         │                                  │
              ┌──────────▼──────────┐          ┌──────────▼──────┐
              │   PostgreSQL 16     │          │   Redis Cache   │
              │   (Multi-tenant)    │          │                 │
              │   + Row-Level       │          └─────────────────┘
              │     Security        │
              └─────────────────────┘
```

---

## Common Commands

### Backend
```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run specific module tests
./gradlew :modules:identity:test

# Check module boundaries
./gradlew verifyModuleBoundaries

# Generate test coverage report
./gradlew jacocoTestReport

# Run the cloud application
./gradlew :app-cloud:bootRun

# Build Docker image
./gradlew :app-cloud:bootBuildImage
```

### Frontend
```bash
# Install all dependencies (from workspace root)
cd web
npm install

# Run all apps in development
npm run dev

# Build all apps for production
npm run build

# Run linting
npm run lint

# Run type checking
npm run type-check

# Run tests
npm run test
```

### Docker
```bash
# Start all services
docker compose up -d

# Stop all services
docker compose down

# View logs
docker compose logs -f

# View specific service logs
docker compose logs -f postgres

# Restart a service
docker compose restart keycloak

# Remove all data (CAUTION: Deletes all data)
docker compose down -v
```

---

## Development Workflow

1. **Start infrastructure** (once per day):
   ```bash
   cd infra/docker && docker compose up -d
   ```

2. **Start backend** (in terminal 1):
   ```bash
   cd backend && ./gradlew :app-cloud:bootRun
   ```

3. **Start frontend** (in terminal 2):
   ```bash
   cd web && npm run dev
   ```

4. **Make changes and test**
   - Backend: Auto-reloads with Spring DevTools
   - Frontend: Auto-reloads with Next.js Fast Refresh

5. **Run tests before committing**:
   ```bash
   ./gradlew test
   npm run test
   ```

---

## Troubleshooting

### Port Already in Use
If you see "port already in use" errors:
```bash
# Find process using port 8081
lsof -i :8081

# Kill the process
kill -9 <PID>
```

### Database Connection Failed
```bash
# Check if PostgreSQL is running
docker compose ps postgres

# Check database logs
docker compose logs postgres

# Restart PostgreSQL
docker compose restart postgres
```

### Gradle Build Fails
```bash
# Clean Gradle cache
./gradlew clean

# Refresh dependencies
./gradlew build --refresh-dependencies
```

### Frontend Build Fails
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

---

## Next Steps

- Read the [Technical Design Document](./Enterprise%20Pharmacy%20Management%20System%20Technical%20Design%20Document.md)
- Review [Architecture Decision Records](./docs/adr/README.md)
- Explore the [API Documentation](http://localhost:8081/swagger-ui.html)
- Join the team chat
- Set up your IDE (IntelliJ IDEA or VS Code recommended)

---

## Getting Help

- Technical issues: Create an issue in the repository
- Questions: Ask in the team chat
- Documentation: Check `/docs` directory
- API reference: `http://localhost:8081/swagger-ui.html` when running

---

**Happy Building! 🚀**
