package com.Omnibus.adapter.out.persistence;

import com.Omnibus.domain.model.Account;
import com.Omnibus.domain.model.AccountStatus;
import com.Omnibus.domain.model.Money;
import org.springframework.stereotype.Component;

/**
 * Maps between Account domain model and AccountJpaEntity.
 */
@Component
public class AccountMapper {

    public Account toDomain(AccountJpaEntity entity) {
        Account account = new Account();
        account.setId(entity.getId());
        account.setUserId(entity.getUserId());
        account.setAccountNumber(entity.getAccountNumber());
        account.setBalance(Money.of(entity.getBalance(), entity.getCurrency()));
        account.setStatus(AccountStatus.valueOf(entity.getStatus()));
        account.setCreatedAt(entity.getCreatedAt());
        account.setUpdatedAt(entity.getUpdatedAt());
        return account;
    }

    public AccountJpaEntity toJpa(Account domain) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setAccountNumber(domain.getAccountNumber());
        entity.setCurrency(domain.getBalance().getCurrency());
        entity.setBalance(domain.getBalance().getAmount());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public void updateJpaFromDomain(Account domain, AccountJpaEntity entity) {
        entity.setBalance(domain.getBalance().getAmount());
        entity.setStatus(domain.getStatus().name());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }
}
