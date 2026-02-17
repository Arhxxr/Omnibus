package com.Omnibus.domain.service;

import com.Omnibus.domain.exception.AccountNotActiveException;
import com.Omnibus.domain.exception.InsufficientFundsException;
import com.Omnibus.domain.model.*;

import java.util.UUID;

/**
 * Pure domain service for executing transfers.
 * Contains the double-entry bookkeeping invariant logic.
 * No Spring annotations — this is pure business logic.
 */
public class TransferDomainService {

    /**
     * Executes a transfer between two accounts using double-entry bookkeeping.
     * Debits the source account and credits the target account.
     *
     * @param source      the source account (must be locked for update)
     * @param target      the target account (must be locked for update)
     * @param amount      the amount to transfer (must be positive)
     * @param transaction the parent transaction record
     * @return a TransferResult containing the two ledger entries
     * @throws InsufficientFundsException  if source balance < amount
     * @throws AccountNotActiveException   if either account is not ACTIVE
     * @throws IllegalArgumentException     if source == target or amount not positive
     */
    public TransferResult executeTransfer(Account source, Account target,
                                          Money amount, Transaction transaction) {
        // ---- Validation ----
        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (!source.isActive()) {
            throw new AccountNotActiveException(
                    "Source account " + source.getId() + " is not active");
        }
        if (!target.isActive()) {
            throw new AccountNotActiveException(
                    "Target account " + target.getId() + " is not active");
        }

        // ---- Double-Entry Bookkeeping ----
        // 1. Debit the source (throws InsufficientFundsException if balance too low)
        Money sourceBalanceAfter = source.debit(amount);

        // 2. Credit the target
        Money targetBalanceAfter = target.credit(amount);

        // 3. Create ledger entries (DEBIT from source, CREDIT to target)
        LedgerEntry debitEntry = new LedgerEntry(
                UUID.randomUUID(),
                transaction.getId(),
                source.getId(),
                EntryType.DEBIT,
                amount,
                sourceBalanceAfter
        );

        LedgerEntry creditEntry = new LedgerEntry(
                UUID.randomUUID(),
                transaction.getId(),
                target.getId(),
                EntryType.CREDIT,
                amount,
                targetBalanceAfter
        );

        // 4. Mark transaction as completed
        transaction.markCompleted();

        return new TransferResult(debitEntry, creditEntry, sourceBalanceAfter, targetBalanceAfter);
    }

    /**
     * Holds the result of a successful transfer: the two ledger entries and updated balances.
     */
    public record TransferResult(
            LedgerEntry debitEntry,
            LedgerEntry creditEntry,
            Money sourceBalanceAfter,
            Money targetBalanceAfter
    ) {
    }
}
