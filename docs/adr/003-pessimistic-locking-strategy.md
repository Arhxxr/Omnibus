# ADR-003: Pessimistic Locking with Deterministic Ordering

**Status:** Accepted  
**Date:** 2026-02-16

## Context

Concurrent transfers between the same accounts can cause **lost updates** (both threads read the same balance, both subtract, one write overwrites the other). Additionally, two simultaneous transfers in opposite directions (A→B and B→A) can **deadlock** if they lock accounts in different orders.

## Decision

We use **pessimistic locking** (`SELECT ... FOR UPDATE`) with **deterministic lock ordering** (ascending UUID).

### Lock Ordering Algorithm

```java
// TransferApplicationService.execute()
List<UUID> sortedIds = new ArrayList<>(List.of(sourceId, targetId));
sortedIds.sort(UUID::compareTo);
List<Account> lockedAccounts = accountRepository.findAllByIdForUpdate(sortedIds);
```

The `findAllByIdForUpdate()` port maps to:
```sql
SELECT * FROM accounts WHERE id IN (?, ?) ORDER BY id FOR UPDATE
```

### Why Deterministic Ordering Prevents Deadlocks

| Thread 1 (A→B) | Thread 2 (B→A) | Without Ordering | With Ordering |
|-----------------|-----------------|------------------|---------------|
| Lock A | Lock B | **DEADLOCK** — each waits for the other | Both lock A first, then B |
| Lock B | Lock A | | Second thread waits until first completes |

By always locking in ascending UUID order (regardless of transfer direction), two transactions involving the same pair of accounts can never form a circular wait.

## Consequences

**Positive:**
- Zero deadlocks under concurrent load (mathematically impossible given the ordering invariant)
- Simple implementation — no retry loops, no optimistic locking version conflicts
- Works with PostgreSQL's row-level locks — no table-level contention

**Negative:**
- Pessimistic locks hold rows for the duration of the transaction — throughput is bounded by lock contention
- Not suitable for extremely high-frequency updates to a single "hot" account (would need sharding or event sourcing)

**Mitigations:**
- Virtual threads (Project Loom) ensure blocked threads don't exhaust the carrier thread pool
- HikariCP pool sized at 20 connections with 5-second timeout prevents connection starvation
- Concurrency stress tests validate lock ordering under load

## Alternatives Considered

| Alternative | Why Rejected |
|-------------|-------------|
| Optimistic locking (`@Version`) | Retry storms under high contention; harder to reason about correctness |
| Serializable isolation | Performance cost too high; locks entire table ranges |
| Event sourcing | Overengineered for this scope; adds CQRS complexity |
