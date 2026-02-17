package com.Omnibus.application.port.out;

import com.Omnibus.domain.model.LedgerEntry;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for ledger entry persistence.
 */
public interface LedgerRepositoryPort {

    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByAccountId(UUID accountId);
}
