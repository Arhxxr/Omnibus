package com.Omnibus.application.port.out;

import com.Omnibus.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for account persistence.
 */
public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    List<Account> findByUserId(UUID userId);

    /**
     * Find and lock multiple accounts using SELECT ... FOR UPDATE ORDER BY id.
     * Accounts are returned in ascending ID order to prevent deadlocks.
     */
    List<Account> findAllByIdForUpdate(List<UUID> ids);

    boolean existsByAccountNumber(String accountNumber);
}
