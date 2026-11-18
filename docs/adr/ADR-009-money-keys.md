# ADR-009 — Money as Integer Minor Units; UTC Timestamps; UUIDv7 Primary Keys

## Status

Accepted (Plan §5.6)

## Context

PharmaPOS handles financial data (prices, payments, taxes, ledger entries) across multiple currencies, timestamps from devices that may have drifted clocks, and primary keys generated offline by Branch Local Nodes and terminals. These three concerns — money, time, and identity — must be handled consistently across the entire codebase to prevent subtle bugs.

## Decision

### Money

All monetary values are stored and transmitted as **integer minor units** (`bigint`) with an explicit **currency code** (ISO 4217, 3 characters).

| Currency | Minor Unit | Storage | Example |
|---|---|---|---|
| UGX (Uganda Shilling) | None in practice (shilling is indivisible) | Whole shillings as `bigint` | 150,000 UGX → `150000` |
| USD | Cent | Cents as `bigint` | $12.50 → `1250` |
| KES | Cent | Cents as `bigint` | KSh 500.00 → `50000` |

The domain layer uses a `Money` value object that encapsulates the amount and currency. No `double`, `float`, or `BigDecimal` is used for money outside of display formatting. Arithmetic operations on `Money` check currency compatibility and reject cross-currency operations without an explicit exchange rate.

The API transmits money as:
```json
{
  "amountMinor": 150000,
  "currency": "UGX"
}
```

### Timestamps

All timestamps are stored as `TIMESTAMPTZ` in **UTC**. Device-local time is stored separately (as `deviceClock`) for audit purposes only. Ordering, comparisons, and business rules always use the UTC server timestamp.

The API transmits timestamps as ISO 8601 strings with explicit UTC offset:
```json
{
  "occurredAt": "2026-11-18T15:30:00Z",
  "deviceClock": "2026-11-18T18:30:00+03:00"
}
```

### Primary Keys

All primary keys are **UUIDv7** (time-ordered UUIDs per RFC 9562). They combine a millisecond-precision timestamp with random bits, providing:

1. **Offline generation:** Terminals and BLNs generate keys without coordinating with the cloud. No sequence gap or collision risk.
2. **Temporal ordering:** Keys sort chronologically, which benefits B-tree indexes and range scans.
3. **Globally unique:** No cross-tenant, cross-branch, or cross-device collision.
4. **Opaque:** Unlike sequential integers, UUIDs do not leak record counts or allow enumeration.

PostgreSQL stores them as the native `UUID` type (16 bytes, indexed efficiently).

## Alternatives Considered

| Alternative | Reason Rejected |
|---|---|
| `DECIMAL` / `NUMERIC` for money | Requires careful scale management; arithmetic bugs are more subtle than with integers; the minor-unit convention is simpler and standard in payment systems |
| `double` or `float` for money | Rounding errors in financial calculations are unacceptable |
| Local timezone storage | Ambiguous during DST transitions; makes cross-branch comparisons error-prone; UTC is unambiguous |
| Auto-increment `BIGINT` primary keys | Cannot be generated offline without coordination; would require range pre-allocation or a central sequence service; reveals record counts |
| UUIDv4 (random) | Not time-ordered; B-tree fragmentation on insert-heavy tables; no temporal information embedded |
| ULID | Similar properties to UUIDv7 but not an IETF standard; UUIDv7 is the RFC 9562 standard and natively supported by PostgreSQL's `UUID` type |

## Consequences

**Positive:**
- No floating-point rounding errors in financial calculations. Ever.
- Cross-currency operations are rejected at the type level, not by convention.
- Timestamps are unambiguous worldwide; no timezone conversion bugs.
- Keys work offline and sort chronologically, optimising both the sync protocol and database indexes.
- The convention is enforced by the `Money` value object and code review; a CI check can flag `double` or `float` in money-related code.

**Negative / Risks:**
- Minor-unit integers require a formatting step for display (dividing by the currency's minor-unit factor). The `Money` value object handles this.
- UUIDv7 keys are 16 bytes vs. 8 bytes for `BIGINT`; at 50M movements/year, this is an additional ~400 MB/year in keys + indexes. Acceptable.
- UGX having no minor unit means the minor-unit factor is 1; this is a special case that the `Money` value object must handle (no division for display).
- Clock skew on devices affects UUIDv7's time component; this does not affect uniqueness (the random bits ensure it) but may affect sort order slightly. The `deviceSeq` in the sync protocol handles ordering independently.

## References

- TDD §4.1
- Plan §5.6
