# Omnibus

A payment gateway I built to learn how money actually moves in real systems. Uses **Java 21**, **Spring Boot 3.5**, **PostgreSQL 16**, and a **React 19 + TypeScript** frontend. Covers double-entry bookkeeping, pessimistic locking, idempotency keys, and an immutable audit trail.

[![Java 21](https://img.shields.io/badge/Java-21_LTS-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![React 19](https://img.shields.io/badge/React-19-blue)](https://react.dev/)
[![Tests](https://img.shields.io/badge/Tests-250-brightgreen)]()

---

## Why This Project Exists

I noticed most tutorial/portfolio payment apps just increment and decrement a balance column. That works for demos, but it falls apart the moment you need to answer questions like `why does this account have this balance?` or `what happens if two transfers hit the same account at the same time?`

I wanted to actually work through the problems that come up when you try to do this properly:

- **Double-entry bookkeeping** - every transfer creates balanced DEBIT/CREDIT entries so the ledger is always auditable
- **Pessimistic locking with deterministic ordering** - `SELECT ... FOR UPDATE` with ascending UUID sort to avoid lost updates and deadlocks
- **Idempotency keys** - exactly-once transfer processing with 24h TTL and cached response replay
- **Immutable audit trail** - `REQUIRES_NEW` propagation so audit entries survive transaction rollbacks
- **Hexagonal architecture** - domain logic has zero framework dependencies, enforced by ArchUnit at build time

This isn't production-ready software. It's a learning project that gave me a much better understanding of how payment systems work under the hood.

---

## Quick Start

### Prerequisites

- **Docker Desktop** (required  - runs PostgreSQL and backend)
- **Node.js 20.19+** (for frontend dev server)

### Run the Full Stack

```bash
# 1. Start PostgreSQL + Spring Boot API
docker compose up -d --build

# 2. Start the React dev server
cd frontend && npm install && npm run dev
```

- **App:** http://localhost:5173
- **API:** http://localhost:8080
- **Swagger:** http://localhost:8080/swagger-ui.html

Register a new account (auto-funded with $10,000). Open a second browser/incognito to create a recipient, then transfer money between them.

### Run Tests

```bash
# Backend  - 159 tests (requires Docker for Testcontainers)
./mvnw clean verify          # Linux/Mac
.\mvnw.cmd clean verify      # Windows

# Frontend  - 91 tests
cd frontend && npm test
```

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│  React SPA (Vite + TypeScript + Tailwind)                │
│  Login → Dashboard → Send Money → Activity → Settings    │
└──────────────────┬───────────────────────────────────────┘
                   │ REST API (/api/v1/*)
┌──────────────────▼───────────────────────────────────────┐
│  Spring Boot API (Hexagonal Architecture)                │
│                                                          │
│  domain/         Pure business logic. No Spring deps.    │
│  application/    Use cases, ports, DTOs                   │
│  adapter/        REST controllers + JPA persistence       │
│  infrastructure/ Security, config, scheduling             │
└──────────────────┬───────────────────────────────────────┘
                   │ JDBC + Flyway
┌──────────────────▼───────────────────────────────────────┐
│  PostgreSQL 16                                           │
│  6 tables · NUMERIC(19,4) money · UUID keys              │
│  Pessimistic row locks · Double-entry ledger             │
└──────────────────────────────────────────────────────────┘
```

**Dependency rule** (enforced by 6 ArchUnit tests):

| Layer | May depend on |
|-------|--------------|
| `domain` | Nothing  - pure Java |
| `application` | `domain` only |
| `adapter` | `application` + `domain` |
| `infrastructure` | All layers |

---

## How Transfers Work

```
POST /api/v1/transfers
Header: Idempotency-Key: <uuid>

1. Check idempotency key → if seen, replay cached response
2. Load source + target accounts (SELECT ... FOR UPDATE, ordered by UUID)
3. Validate: ownership, active status, sufficient funds, currency match
4. Execute double-entry bookkeeping:
   - DEBIT  source  → balance_after recorded
   - CREDIT target  → balance_after recorded
5. Update account balances atomically
6. Cache response against idempotency key (24h TTL)
7. Write audit log (REQUIRES_NEW  - survives rollback)
```

Concurrent stress tests verify this works correctly with 20 parallel threads hammering the same accounts.

---

## API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` |  - | Create account, returns JWT |
| `POST` | `/api/v1/auth/login` |  - | Authenticate, returns JWT |
| `GET` | `/api/v1/auth/me` | JWT | User profile + accounts |
| `GET` | `/api/v1/accounts` | JWT | List user's accounts |
| `GET` | `/api/v1/accounts/{id}` | JWT | Account detail (ownership verified) |
| `GET` | `/api/v1/accounts/{id}/transactions` | JWT | Transaction history |
| `GET` | `/api/v1/accounts/lookup?username=` | JWT | Recipient lookup |
| `POST` | `/api/v1/transfers` | JWT | Execute transfer (idempotency key required) |

All error responses follow [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) Problem Detail format. Interactive docs at `/swagger-ui.html`.

---

## Tech Stack

### Backend
| | |
|---|---|
| Java 21 LTS | Virtual threads, ZGC, records |
| Spring Boot 3.5 | Web, Data JPA, Security, Validation, Actuator |
| PostgreSQL 16 | Flyway migrations, `NUMERIC(19,4)` money, pessimistic locks |
| JJWT 0.12 | HMAC-SHA256 JWT with 15-min expiry |
| Testcontainers | Integration tests against real PostgreSQL |
| ArchUnit | Build-time architecture enforcement |

### Frontend
| | |
|---|---|
| React 19 + TypeScript 5.9 | Type-safe components and API contracts |
| Vite 6 | Dev proxy, HMR, chunk splitting |
| TanStack Query 5 | Server state with automatic cache invalidation |
| Zod + React Hook Form | Schema validation matching backend constraints |
| Tailwind CSS 3.4 | Dark/light theming via CSS custom properties |
| Vitest + Testing Library | 91 component and integration tests |

---

## Test Suite

250 tests across backend and frontend:

| Category | Tests | Scope |
|----------|-------|-------|
| Domain model | 47 | Money arithmetic, account invariants, entity validation |
| Domain service | 7 | Double-entry bookkeeping logic |
| Auth | 13 | Register, login, password hashing, JWT generation |
| Transfer integration | 30 | End-to-end transfers, concurrent stress, edge cases |
| Idempotency | 10 | Replay, concurrent same-key, TTL, cross-user isolation |
| Validation & errors | 22 | Input validation, malformed requests, RFC 7807 format |
| Architecture | 6 | ArchUnit layer dependency enforcement |
| JWT + scheduling | 16 | Token lifecycle, idempotency key cleanup |
| Frontend | 91 | Auth flows, dashboard, send wizard, activity, forms |

---

## Project Structure

```
src/main/java/com/omnibus/
├── domain/
│   ├── model/           Account, Transaction, Money (value object), LedgerEntry, User
│   ├── exception/       InsufficientFunds, AccountNotFound, Ownership, Idempotency
│   └── service/         TransferDomainService  - pure double-entry logic
├── application/
│   ├── port/in/         Use case interfaces (Auth, Transfer, Account)
│   ├── port/out/        Repository + infrastructure port interfaces
│   ├── service/         Use case implementations
│   └── dto/             Commands + response records
├── adapter/
│   ├── in/web/          REST controllers + global exception handler
│   └── out/persistence/ JPA entities, repositories, mappers, adapters
└── infrastructure/
    ├── config/          Security, OpenAPI, domain bean wiring
    ├── security/        JWT filter, token provider, password encoder
    ├── scheduling/      Idempotency key TTL cleanup (hourly)
    └── audit/           Immutable audit log service (REQUIRES_NEW)

frontend/src/
├── pages/               Login, Register, Dashboard, SendMoney, Activity, Settings
├── components/          ErrorBoundary, ProtectedRoute, Sidebar, reusable UI
├── contexts/            AuthContext  - JWT lifecycle, auto-logout on 401
├── hooks/               useTheme  - dark/light with localStorage persistence
├── lib/                 Axios client with JWT interceptor, utility functions
└── types/               TypeScript interfaces for all API contracts
```

---

## Security

Full OWASP Top 10 audit documented in [SECURITY.md](SECURITY.md) with honest PASS/PARTIAL ratings.

Highlights:
- JWT (HMAC-SHA256, 15-min expiry) + BCrypt passwords
- Account ownership enforced on all authenticated endpoints (prevents IDOR)
- Production profile externalizes secrets, disables Swagger, suppresses stack traces
- Non-root Docker container (`appuser:1001`), ZGC, virtual threads
- Frontend: Zod validation mirrors backend constraints, 401 auto-logout, error boundary

---

## Docker

Three-stage Dockerfile: **Node 20** (frontend build) → **JDK 21** (Maven build + static assets) → **JRE 21 Alpine** (runtime).

```bash
docker compose up -d --build    # Start PostgreSQL + app
docker compose down             # Tear down
```

Runtime: non-root user, ZGC garbage collector, 75% max RAM, Alpine base.

---

## Design Decisions

Documented as Architecture Decision Records in [`docs/adr/`](docs/adr/):

| ADR | Decision |
|-----|----------|
| [001](docs/adr/001-hexagonal-architecture.md) | Hexagonal architecture with ArchUnit enforcement |
| [002](docs/adr/002-double-entry-bookkeeping.md) | Double-entry bookkeeping with `BigDecimal` HALF_EVEN precision |
| [003](docs/adr/003-pessimistic-locking-strategy.md) | Pessimistic locking + deterministic UUID ordering for deadlock prevention |
| [004](docs/adr/004-tech-stack-versions.md) | Dependency audit  - Testcontainers/Docker API compatibility, Hibernate 6.6 strict validation |
| [005](docs/adr/005-react-frontend-architecture.md) | React SPA migration  - TanStack Query, Zod validation, JWT auth flow |

---

## Load Testing

[Locust](https://locust.io/) scripts in [`load-tests/`](load-tests/) with three user profiles: realistic payment mix, idempotency stress, and burst throughput.

```bash
cd load-tests && pip install -r requirements.txt
locust -f locustfile.py --host http://localhost:8080
```
