# ADR-004: Tech Stack Version Decisions

**Status:** Accepted  
**Date:** 2026-02-16

## Context

Omnibus targets **current fintech standards**. All dependency versions were audited and upgraded from initial selections to the latest stable releases compatible with the project's requirements.

## Decision

### Version Upgrades Applied

| Dependency | Initial | Final | Rationale |
|------------|---------|-------|-----------|
| Spring Boot | 3.2.5 | 3.5.10 | Latest LTS-equivalent release; includes Hibernate 6.6, Flyway 10 |
| Testcontainers | 1.21.0 | 1.21.4 | **Critical fix:** 1.21.0 hardcodes Docker API `v1.32`; Docker Engine 29 requires `v1.44` minimum. 1.21.4 (Dec 15, 2025) adds proper API version negotiation. |
| JJWT | 0.12.5 | 0.12.6 | Latest stable |
| SpringDoc | 2.4.0 | 2.7.0 | Latest stable |
| ArchUnit | 1.2.1 | 1.3.0 | Latest stable |
| Flyway | 9.x | 10.x | Via Spring Boot 3.5.10; required adding `flyway-database-postgresql` module |
| Hibernate | 6.4.x | 6.6.x | Via Spring Boot 3.5.10; stricter schema validation (SQL types must exactly match JPA annotations) |

### Key Compatibility Issues Resolved

1. **Testcontainers ↔ Docker Engine 29:** Root cause was API version negotiation. Testcontainers 1.21.0 sends `GET /v1.32/info` which returns HTTP 400 on Engine 29. Verified: `GET /v1.44/info` returns 200. Upgrade to 1.21.4 resolved.

2. **Flyway 10 module split:** Flyway 10 extracted database-specific code into separate modules. Added `flyway-database-postgresql` alongside `flyway-core`.

3. **Hibernate 6.6 strict validation:** With `ddl-auto=validate`, Hibernate 6.6 rejects mismatches between SQL column types and JPA `@Column` annotations. Fixed two mismatches:
   - `accounts.currency`: SQL `CHAR(3)` → `VARCHAR(3)` to match JPA `@Column(length=3)`
   - `idempotency_keys.http_status`: SQL `SMALLINT` → `INTEGER` to match JPA `Integer` mapping

## Consequences

- Full test suite passes on clean build with zero workarounds
- Stack is current — no deprecated APIs in use
- Testcontainers works with Docker Desktop latest (Engine 29.x)
