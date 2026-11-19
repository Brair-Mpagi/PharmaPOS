# ADR-001: Modular Monolith Architecture

## Status
Accepted

## Context
PharmaPOS needs to be built quickly with a small team while maintaining clear module boundaries for future scalability. We need to choose between:
- Microservices from day one
- Traditional monolith
- Modular monolith

## Decision
We will build a **modular monolith** using Spring Modulith, with clearly defined bounded contexts that could be extracted as microservices if needed.

### Key Principles
1. One Git repository, one deployment unit initially
2. Modules organized by bounded context (Catalogue, Inventory, Sales, etc.)
3. Each module has public API package and internal implementation
4. Spring Modulith enforces module boundaries at build time
5. Inter-module communication via events (transactional outbox)
6. No direct database access across modules

### Module Structure
```
backend/modules/
  ├── shared-kernel/      # Common value objects, base classes
  ├── catalogue/          # Product and pricing
  ├── inventory/          # Stock management
  ├── sales/              # POS and transactions
  └── [other domains]/
```

## Consequences

### Positive
- **Faster initial development**: Single deployment, no distributed system complexity
- **Strong boundaries**: Spring Modulith verifies module dependencies at build time
- **Easy debugging**: All code in one process, no distributed tracing needed initially
- **Transactional consistency**: Critical flows (sale + stock decrement) are ACID
- **Future flexibility**: Well-defined modules can be extracted later if needed
- **Small team friendly**: Easier to understand and maintain than microservices

### Negative
- **Scaling limits**: Cannot scale modules independently (mitigated by horizontal scaling)
- **Blast radius**: A bug in one module could affect others (mitigated by good testing)
- **Deployment coupling**: All modules deploy together (mitigated by feature flags)

### Neutral
- **Eventual extraction**: If we grow to need microservices, the module boundaries are already defined
- **Event-driven patterns**: We use events even within the monolith, preparing for future distribution
