package com.Omnibus.adapter.out.persistence;

import com.Omnibus.application.port.out.AccountRepositoryPort;
import com.Omnibus.domain.model.Account;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repository;
    private final AccountMapper mapper;
    private final EntityManager entityManager;

    public AccountPersistenceAdapter(AccountJpaRepository repository,
                                     AccountMapper mapper,
                                     EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = mapper.toJpa(account);
        AccountJpaEntity managed = entityManager.merge(entity);
        return mapper.toDomain(managed);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Account> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Account> findAllByIdForUpdate(List<UUID> ids) {
        return repository.findAllByIdForUpdate(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return repository.existsByAccountNumber(accountNumber);
    }
}
