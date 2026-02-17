package com.Omnibus.adapter.out.persistence;

import com.Omnibus.application.port.out.LedgerRepositoryPort;
import com.Omnibus.domain.model.LedgerEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class LedgerPersistenceAdapter implements LedgerRepositoryPort {

    private final LedgerEntryJpaRepository repository;
    private final TransactionMapper mapper;

    public LedgerPersistenceAdapter(LedgerEntryJpaRepository repository, TransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryJpaEntity entity = mapper.ledgerToJpa(entry);
        LedgerEntryJpaEntity saved = repository.save(entity);
        return mapper.ledgerToDomain(saved);
    }

    @Override
    public List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return repository.findByTransactionId(transactionId).stream()
                .map(mapper::ledgerToDomain)
                .toList();
    }

    @Override
    public List<LedgerEntry> findByAccountId(UUID accountId) {
        return repository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(mapper::ledgerToDomain)
                .toList();
    }
}
