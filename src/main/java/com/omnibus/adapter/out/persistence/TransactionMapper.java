package com.Omnibus.adapter.out.persistence;

import com.Omnibus.domain.model.*;
import org.springframework.stereotype.Component;

/**
 * Maps between Transaction/LedgerEntry domain models and their JPA entities.
 */
@Component
public class TransactionMapper {

    public Transaction toDomain(TransactionJpaEntity entity) {
        Transaction txn = new Transaction();
        txn.setId(entity.getId());
        txn.setIdempotencyKey(entity.getIdempotencyKey());
        txn.setType(TransactionType.valueOf(entity.getType()));
        txn.setStatus(TransactionStatus.valueOf(entity.getStatus()));
        txn.setSourceAccountId(entity.getSourceAccountId());
        txn.setTargetAccountId(entity.getTargetAccountId());
        // Amount requires a currency; we'll use USD as default for the DTO layer
        txn.setAmount(Money.of(entity.getAmount(), "USD"));
        txn.setDescription(entity.getDescription());
        txn.setCreatedAt(entity.getCreatedAt());
        txn.setCompletedAt(entity.getCompletedAt());
        return txn;
    }

    public TransactionJpaEntity toJpa(Transaction domain) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(domain.getId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setType(domain.getType().name());
        entity.setStatus(domain.getStatus().name());
        entity.setSourceAccountId(domain.getSourceAccountId());
        entity.setTargetAccountId(domain.getTargetAccountId());
        entity.setAmount(domain.getAmount().getAmount());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCompletedAt(domain.getCompletedAt());
        return entity;
    }

    public void updateJpaFromDomain(Transaction domain, TransactionJpaEntity entity) {
        entity.setStatus(domain.getStatus().name());
        entity.setCompletedAt(domain.getCompletedAt());
    }

    public LedgerEntry ledgerToDomain(LedgerEntryJpaEntity entity) {
        LedgerEntry entry = new LedgerEntry();
        entry.setId(entity.getId());
        entry.setTransactionId(entity.getTransactionId());
        entry.setAccountId(entity.getAccountId());
        entry.setEntryType(EntryType.valueOf(entity.getEntryType()));
        entry.setAmount(Money.of(entity.getAmount(), "USD"));
        entry.setBalanceAfter(Money.of(entity.getBalanceAfter(), "USD"));
        entry.setCreatedAt(entity.getCreatedAt());
        return entry;
    }

    public LedgerEntryJpaEntity ledgerToJpa(LedgerEntry domain) {
        LedgerEntryJpaEntity entity = new LedgerEntryJpaEntity();
        entity.setId(domain.getId());
        entity.setTransactionId(domain.getTransactionId());
        entity.setAccountId(domain.getAccountId());
        entity.setEntryType(domain.getEntryType().name());
        entity.setAmount(domain.getAmount().getAmount());
        entity.setBalanceAfter(domain.getBalanceAfter().getAmount());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
