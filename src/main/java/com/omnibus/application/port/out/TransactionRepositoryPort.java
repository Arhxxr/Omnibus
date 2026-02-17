package com.Omnibus.application.port.out;

import com.Omnibus.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for transaction persistence.
 */
public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all transactions where the given account is either source or target.
     * Results are ordered by creation date descending.
     */
    List<Transaction> findByAccountId(UUID accountId);
}
