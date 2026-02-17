# ADR-001: Hexagonal Architecture

**Status:** Accepted  
**Date:** 2026-02-16

## Context

Omnibus is a financial transaction processor that must be:
- Rigorously testable (domain logic must be tested without a running database or Spring context)
- Maintainable by senior engineers who expect clean separation of concerns
- Able to swap infrastructure (e.g., replace PostgreSQL with another store) without touching business logic

## Decision

We adopt **Hexagonal Architecture** (Ports & Adapters), organized into four package layers:

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Domain | `com.Omnibus.domain` | Pure business logic — entities, value objects, domain services, exceptions. No framework annotations. |
| Application | `com.Omnibus.application` | Use-case orchestration, DTOs, port interfaces (inbound + outbound). Depends only on domain. |
| Adapter | `com.Omnibus.adapter` | REST controllers (inbound) and JPA persistence (outbound). Depends on application + domain. |
| Infrastructure | `com.Omnibus.infrastructure` | Spring Security config, JWT provider, audit wiring. Bridges the framework to the architecture. |

**Dependency rules are enforced at build time** by 6 ArchUnit tests in `ArchitectureTest.java`.

## Consequences

**Positive:**
- Domain logic (`Money`, `Account.debit()`, `TransferDomainService`) has zero Spring dependencies — tested with plain JUnit + Mockito
- 26 domain/unit tests run in < 1 second, no containers needed
- Clear boundaries make code review straightforward
- Persistence strategy can be changed without touching domain or application layers

**Negative:**
- More files than a traditional layered architecture (~70 source files)
- Mapper boilerplate between domain entities and JPA entities
- New developers must understand the port/adapter pattern

**Mitigations:**
- Mapper classes are simple, stateless, and follow a consistent pattern
- Package structure is self-documenting with `package-info.java`
