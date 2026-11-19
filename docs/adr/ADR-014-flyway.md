# ADR-014: Flyway for Database Migrations

## Status
Accepted

## Context
Need to choose a database migration tool. Two main options:
- **Flyway**: SQL-based, simple, widely adopted
- **Liquibase**: XML/YAML/JSON/SQL, more features, more complex

## Decision
Use **Flyway** for database migrations.

### Rationale
1. **Simplicity**: Pure SQL files, easy to review and understand
2. **Git-friendly**: SQL diffs are readable
3. **Team skills**: SQL is universal, no DSL to learn
4. **Spring Boot integration**: First-class support, auto-configuration
5. **Multi-tenancy support**: Can loop over tenant directory
6. **Sufficient for our needs**: We don't need advanced Liquibase features

### Migration Structure
```
modules/[module-name]/src/main/resources/db/migration/
  V001__initial_[module]_schema.sql
  V002__add_batch_quarantine.sql
  V003__index_optimization.sql
```

### Naming Convention
- Version: `V{number}` (e.g., V001, V002)
- Separator: `__` (double underscore)
- Description: snake_case, descriptive
- Example: `V001__initial_identity_schema.sql`

### Best Practices
1. **One migration per feature**: Keep migrations focused
2. **No rollbacks**: Only forward migrations (use compensating migrations)
3. **Expand-and-contract**: Additive changes first, removals later
4. **Test in isolation**: Every migration runs on a clean database in CI
5. **RLS required**: Build fails if table missing `tenant_id` and RLS policy

## Consequences

### Positive
- **Easy to learn**: Plain SQL, no XML or YAML
- **Easy to review**: Code reviews see actual SQL changes
- **Version control friendly**: Clean diffs
- **Debugging**: Failed migrations show clear SQL errors
- **Performance**: Baseline and repair commands handle edge cases

### Negative
- **Less powerful than Liquibase**: No changeset rollback, no database abstraction
- **Manual RLS**: Must write RLS policies by hand (mitigated by helpers and tests)

### Neutral
- **Sufficient for v1**: Can revisit if needs change
- **Industry standard**: Most teams use either Flyway or Liquibase successfully
