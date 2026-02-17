package com.Omnibus.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a single ledger entry (one half of a double-entry pair).
 * Every financial movement produces exactly one DEBIT and one CREDIT entry.
 */
public class LedgerEntry {

    private UUID id;
    private UUID transactionId;
    private UUID accountId;
    private EntryType entryType;
    private Money amount;
    private Money balanceAfter;
    private Instant createdAt;

    /** JPA / mapper use only. */
    public LedgerEntry() {
    }

    public LedgerEntry(UUID id, UUID transactionId, UUID accountId,
                       EntryType entryType, Money amount, Money balanceAfter) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.entryType = Objects.requireNonNull(entryType, "entryType must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "balanceAfter must not be null");
        this.createdAt = Instant.now();
    }

    // ---- Getters & Setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Money balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
