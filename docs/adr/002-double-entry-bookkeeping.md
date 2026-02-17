# ADR-002: Double-Entry Bookkeeping

**Status:** Accepted  
**Date:** 2026-02-16

## Context

Financial systems require an auditable, provably correct record of every money movement. A naive approach (just updating account balances) makes it impossible to trace errors, detect fraud, or prove that the system's total money supply is conserved.

## Decision

Every transfer creates **exactly two ledger entries**: one DEBIT and one CREDIT.

```
Transfer $500 from Account A → Account B:

  ledger_entries:
  ┌──────────────────┬────────────┬──────────┬────────┬───────────────┐
  │ transaction_id   │ account_id │ type     │ amount │ balance_after │
  ├──────────────────┼────────────┼──────────┼────────┼───────────────┤
  │ txn-001          │ acct-A     │ DEBIT    │ 500    │ 9,500         │
  │ txn-001          │ acct-B     │ CREDIT   │ 500    │ 10,500        │
  └──────────────────┴────────────┴──────────┴────────┴───────────────┘
```

**Implementation:**
- `TransferDomainService.executeTransfer()` — pure domain logic, no Spring
- `LedgerEntry` — immutable domain entity capturing one side of the double entry
- `balance_after` column on each entry enables point-in-time balance reconstruction
- All amounts use `BigDecimal` with `HALF_EVEN` rounding and scale 4 (`NUMERIC(19,4)` in SQL)

## Consequences

**Positive:**
- **Invariant enforcement:** `SUM(DEBIT amounts) == SUM(CREDIT amounts)` across all entries — verifiable by a single SQL query
- **Complete audit trail:** Every balance change is traceable to a specific transaction and timestamp
- **Balance reconstruction:** Can rebuild any account's balance from the ledger entries alone
- **Regulatory compliance:** Standard accounting practice required by financial regulations

**Negative:**
- Double the write volume compared to simple balance updates
- Queries against `ledger_entries` need proper indexing (provided: `idx_ledger_entries_txn`, `idx_ledger_entries_account`)

## Verification

Integration tests assert: for every completed transaction, exactly one DEBIT and one CREDIT entry exist with equal amounts.
