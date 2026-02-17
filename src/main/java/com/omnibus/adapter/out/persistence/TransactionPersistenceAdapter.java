package com.Omnibus.adapter.out.persistence;

import com.Omnibus.application.port.out.TransactionRepositoryPort;
import com.Omnibus.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository repository;
    private final TransactionMapper mapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository repository, TransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = mapper.toJpa(transaction);
        TransactionJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        return repository.findBySourceAccountIdOrTargetAccountId(accountId, accountId)
                .stream()
                .map(mapper::toDomain)
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .toList();
    }
}
