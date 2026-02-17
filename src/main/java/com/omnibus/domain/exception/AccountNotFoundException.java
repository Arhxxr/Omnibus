package com.Omnibus.domain.exception;

import java.util.UUID;

/**
 * Thrown when a referenced account does not exist.
 */
public class AccountNotFoundException extends DomainException {

    private final UUID accountId;

    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
