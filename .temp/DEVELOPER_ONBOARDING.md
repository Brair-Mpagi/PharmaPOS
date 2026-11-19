# PharmaPOS Developer Onboarding Guide

Welcome to the PharmaPOS development team! This guide will get you productive quickly.

## 🎯 Day 1: Environment Setup

### 1. Install Prerequisites

**Required Software:**
- [ ] **Java 25 JDK** - [Download](https://www.oracle.com/java/technologies/downloads/#java25) or use SDKMAN
- [ ] **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- [ ] **Node.js 22 LTS** - [Download](https://nodejs.org/)
- [ ] **Git** - [Download](https://git-scm.com/)
- [ ] **IDE:** IntelliJ IDEA (recommended) or VS Code

**Verify installations:**
```bash
java -version    # Should show Java 25
docker --version # Should show 24.0+
node --version   # Should show v22.x
git --version    # Should show 2.x+
```

### 2. Clone and Setup

```bash
# Clone repository
git clone <repository-url>
cd PharmaPOS

# Start infrastructure
cd infra/docker
docker compose up -d

# Verify services are healthy
docker compose ps
```

### 3. First Build

```bash
# Backend
cd backend
./gradlew clean build

# Frontend (optional for backend devs)
cd ../web
npm install
```

### 4. Run the System

```bash
# Terminal 1: Backend
cd backend
./gradlew :app-cloud:bootRun

# Terminal 2: Frontend (optional)
cd web
npm run dev
```

**Test it works:**
```bash
curl http://localhost:8081/api/v1/health
# Should return: {"status":"UP",...}
```

✅ **Day 1 Goal:** You can start the system and see it running.

---

## 🎯 Day 2-3: Codebase Understanding

### Project Structure Overview

```
PharmaPOS/
├── backend/                    # Spring Boot modular monolith
│   ├── modules/               # 14 bounded context modules
│   │   ├── shared-kernel/    # Value objects, base classes
│   │   ├── identity/         # Users, roles, permissions
│   │   ├── catalogue/        # Products, pricing
│   │   ├── inventory/        # Stock, batches, movements
│   │   └── sales/            # POS, shifts, payments
│   ├── app-cloud/            # Cloud deployment assembly
│   └── app-edge/             # Branch Local Node assembly
├── web/                       # Frontend applications
│   ├── apps/terminal/        # POS terminal PWA
│   ├── apps/backoffice/      # Management web app
│   └── apps/platform/        # Platform admin console
├── contracts/                 # OpenAPI specifications
├── infra/                     # Infrastructure as code
└── docs/                      # Documentation and ADRs
```

### Key Architectural Concepts

#### 1. Modular Monolith (ADR-001)
- One codebase, clear module boundaries
- Spring Modulith enforces separation
- Each module = one bounded context
- Public API packages, internal implementation

#### 2. Multi-Tenancy (ADR-002)
- Pooled database with Row-Level Security
- Every request is tenant-scoped
- TenantContext holds tenant_id, user_id, roles
- Database enforces isolation

#### 3. Module Communication
- **Synchronous:** Direct in-process calls within transaction
- **Asynchronous:** Events via transactional outbox
- **Never:** Direct database access across modules

#### 4. Value Objects
- **Money:** Integer minor units (no floating point!)
- **Quantity:** Decimal with unit of measure
- **Always immutable, always currency/unit aware**

### Reading Order

**Day 2: Foundation**
1. [README.md](./README.md) - Project overview
2. [QUICKSTART.md](./QUICKSTART.md) - Setup guide
3. [ADR-001](./docs/adr/ADR-001-modular-monolith.md) - Architecture
4. [ADR-002](./docs/adr/ADR-002-hybrid-tenancy.md) - Multi-tenancy
5. [OpenAPI spec](./contracts/openapi/pharmapos-api.yaml) - API contract

**Day 3: Deep Dive**
1. [Technical Design Document](./Enterprise%20Pharmacy%20Management%20System%20Technical%20Design%20Document.md)
2. Explore `backend/modules/shared-kernel/` - Base classes
3. Review `backend/modules/identity/` - IAM module
4. Study database migration: `V001__initial_identity_schema.sql`

✅ **Day 2-3 Goal:** Understand the architecture and design patterns.

---

## 🎯 Week 1: First Contribution

### Module Development Workflow

Let's implement a simple feature in the Catalogue module.

#### Step 1: Understand the Module

```bash
cd backend/modules/catalogue
```

**Module Structure:**
```
catalogue/
├── src/main/java/com/pharmapos/catalogue/
│   ├── api/                   # Public interfaces (other modules can use)
│   │   ├── CatalogueApi.java
│   │   └── dto/
│   └── internal/              # Implementation (private to module)
│       ├── domain/           # Entities, value objects, aggregates
│       ├── application/      # Use case services
│       ├── persistence/      # Repositories
│       └── web/              # REST controllers
└── src/main/resources/
    └── db/migration/          # Flyway migrations
```

#### Step 2: Create a Database Migration

```sql
-- V001__initial_catalogue_schema.sql
CREATE TABLE cat_sellable_item (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    base_unit VARCHAR(10) NOT NULL,
    requires_prescription BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT cat_sellable_item_status_check 
        CHECK (status IN ('ACTIVE', 'DISCONTINUED', 'SUSPENDED'))
);

-- Row-Level Security
ALTER TABLE cat_sellable_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE cat_sellable_item FORCE ROW LEVEL SECURITY;

CREATE POLICY cat_sellable_item_tenant_isolation ON cat_sellable_item
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- Indexes
CREATE INDEX idx_cat_sellable_item_tenant ON cat_sellable_item(tenant_id);
CREATE INDEX idx_cat_sellable_item_name ON cat_sellable_item(tenant_id, name);
```

#### Step 3: Create Domain Entity

```java
package com.pharmapos.catalogue.internal.domain;

import com.pharmapos.shared.domain.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cat_sellable_item")
@Getter
@Setter
public class SellableItem extends TenantEntity {
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "generic_name")
    private String genericName;
    
    @Column(name = "base_unit", nullable = false, length = 10)
    private String baseUnit;
    
    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemStatus status = ItemStatus.ACTIVE;
    
    public enum ItemStatus {
        ACTIVE, DISCONTINUED, SUSPENDED
    }
}
```

#### Step 4: Create Repository

```java
package com.pharmapos.catalogue.internal.persistence;

import com.pharmapos.catalogue.internal.domain.SellableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SellableItemRepository extends JpaRepository<SellableItem, UUID> {
    
    List<SellableItem> findByTenantIdAndStatus(
        UUID tenantId, 
        SellableItem.ItemStatus status
    );
    
    List<SellableItem> findByTenantIdAndNameContainingIgnoreCase(
        UUID tenantId,
        String nameFragment
    );
}
```

#### Step 5: Create Service

```java
package com.pharmapos.catalogue.internal.application;

import com.pharmapos.catalogue.internal.domain.SellableItem;
import com.pharmapos.catalogue.internal.persistence.SellableItemRepository;
import com.pharmapos.shared.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogueService {
    
    private final SellableItemRepository itemRepository;
    
    public List<SellableItem> searchItems(String query) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        return itemRepository.findByTenantIdAndNameContainingIgnoreCase(
            tenantId, query
        );
    }
    
    @Transactional
    public SellableItem createItem(SellableItem item) {
        UUID tenantId = TenantContextHolder.getContext().getTenantId();
        item.setTenantId(tenantId);
        return itemRepository.save(item);
    }
}
```

#### Step 6: Create REST Controller

```java
package com.pharmapos.catalogue.internal.web;

import com.pharmapos.catalogue.internal.application.CatalogueService;
import com.pharmapos.catalogue.internal.domain.SellableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogue")
@RequiredArgsConstructor
public class CatalogueController {
    
    private final CatalogueService catalogueService;
    
    @GetMapping("/items")
    @PreAuthorize("hasAuthority('catalogue.read')")
    public ResponseEntity<List<SellableItem>> searchItems(
            @RequestParam(required = false) String query) {
        List<SellableItem> items = catalogueService.searchItems(query);
        return ResponseEntity.ok(items);
    }
    
    @PostMapping("/items")
    @PreAuthorize("hasAuthority('catalogue.write')")
    public ResponseEntity<SellableItem> createItem(@RequestBody SellableItem item) {
        SellableItem created = catalogueService.createItem(item);
        return ResponseEntity.ok(created);
    }
}
```

#### Step 7: Write Tests

```java
package com.pharmapos.catalogue.internal.application;

import com.pharmapos.catalogue.internal.domain.SellableItem;
import com.pharmapos.catalogue.internal.persistence.SellableItemRepository;
import com.pharmapos.shared.context.TenantContext;
import com.pharmapos.shared.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogueServiceTest {
    
    @Mock
    private SellableItemRepository itemRepository;
    
    private CatalogueService service;
    
    private UUID testTenantId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        service = new CatalogueService(itemRepository);
        
        // Set up tenant context for tests
        TenantContext context = TenantContext.builder()
            .tenantId(testTenantId)
            .userId(UUID.randomUUID())
            .username("test-user")
            .build();
        TenantContextHolder.setContext(context);
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void createItem_shouldSetTenantIdFromContext() {
        // Given
        SellableItem item = new SellableItem();
        item.setName("Paracetamol 500mg");
        item.setBaseUnit("TAB");
        
        when(itemRepository.save(any(SellableItem.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // When
        SellableItem created = service.createItem(item);
        
        // Then
        assertThat(created.getTenantId()).isEqualTo(testTenantId);
        verify(itemRepository).save(item);
    }
}
```

#### Step 8: Run Migration and Test

```bash
# Run migration
./gradlew :app-cloud:flywayMigrate

# Run tests
./gradlew :modules:catalogue:test

# Start app and test endpoint
./gradlew :app-cloud:bootRun

# In another terminal
curl http://localhost:8081/api/v1/catalogue/items?query=para \
  -H "Authorization: Bearer <token>"
```

✅ **Week 1 Goal:** Complete your first feature end-to-end.

---

## 🔧 Development Best Practices

### 1. Always Set Tenant Context in Tests
```java
@BeforeEach
void setUp() {
    TenantContext context = TenantContext.builder()
        .tenantId(UUID.randomUUID())
        .build();
    TenantContextHolder.setContext(context);
}

@AfterEach
void tearDown() {
    TenantContextHolder.clear();
}
```

### 2. Use Value Objects for Money and Quantity
```java
// ✅ Good
Money unitPrice = Money.fromMajor(BigDecimal.valueOf(10.50), "UGX");
Money total = unitPrice.multiply(quantity);

// ❌ Bad - Never use double for money!
double price = 10.50;
double total = price * quantity;
```

### 3. Never Access Another Module's Tables
```java
// ✅ Good - Call through API
CatalogueApi catalogueApi;
SellableItem item = catalogueApi.getItem(itemId);

// ❌ Bad - Direct database access
EntityManager em;
em.find(com.pharmapos.catalogue.internal.domain.SellableItem.class, itemId);
```

### 4. Every Endpoint Needs Permission Check
```java
@PreAuthorize("hasAuthority('sales.create')")
public ResponseEntity<Sale> createSale(@RequestBody SaleRequest request) {
    // ...
}
```

### 5. Use Transactions Properly
```java
@Service
@Transactional(readOnly = true)  // Default for all methods
public class MyService {
    
    @Transactional  // Override for write methods
    public void saveData() {
        // ...
    }
}
```

---

## 🧪 Testing Guidelines

### Test Pyramid
- **70% Unit Tests:** Fast, isolated, mock dependencies
- **20% Integration Tests:** Test module boundaries, database
- **10% E2E Tests:** Full stack, critical user journeys

### Running Tests
```bash
# All tests
./gradlew test

# Specific module
./gradlew :modules:catalogue:test

# Integration tests only
./gradlew integrationTest

# With coverage
./gradlew test jacocoTestReport

# Critical module coverage check (fails if <80%)
./gradlew verifyCriticalCoverage
```

---

## 🐛 Debugging Tips

### Enable SQL Logging
In `application.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### View Tenant Context
Add breakpoint in `TenantContextFilter.doFilterInternal()`

### Database Queries
```bash
# Connect to database
docker exec -it pharmapos-postgres psql -U postgres -d pharmapos_pool

# View tenant data
SET app.tenant_id = '<tenant-uuid>';
SELECT * FROM cat_sellable_item;
```

---

## 📚 Additional Resources

### Internal
- [Technical Design Document](./Enterprise%20Pharmacy%20Management%20System%20Technical%20Design%20Document.md)
- [All ADRs](./docs/adr/README.md)
- [API Contract](./contracts/openapi/pharmapos-api.yaml)
- [Build Verification](./BUILD_VERIFICATION.md)

### External
- [Spring Modulith Docs](https://spring.io/projects/spring-modulith)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)
- [PostgreSQL Row-Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)

---

## 🤝 Getting Help

### Quick Questions
- **Team chat:** #pharmapos-dev
- **Code reviews:** GitHub pull requests
- **Bugs:** GitHub issues

### Detailed Questions
- **Architecture:** Review ADRs, then ask in #architecture
- **Security:** Ask in #security channel
- **DevOps:** Ask in #infrastructure

### Stuck on Something?
1. Check BUILD_VERIFICATION.md
2. Review relevant ADR
3. Search codebase for similar code
4. Ask team lead

---

## ✅ Onboarding Checklist

### Week 1
- [ ] Environment set up and system running
- [ ] Read all Day 1-3 documentation
- [ ] Understand module structure
- [ ] Complete first feature (guided)
- [ ] First PR merged

### Week 2
- [ ] Independent feature implementation
- [ ] Write comprehensive tests
- [ ] Participate in code review
- [ ] Fix a bug from backlog

### Month 1
- [ ] Contribute to multiple modules
- [ ] Propose an improvement
- [ ] Help onboard next developer
- [ ] Fully productive team member

---

**Welcome aboard! 🚀**
