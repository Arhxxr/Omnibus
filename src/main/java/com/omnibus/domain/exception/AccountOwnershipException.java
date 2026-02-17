package com.Omnibus.domain.exception;

import java.util.UUID;

/**
 * Thrown when a user attempts to access or operate on an account they do not own.
 */
public class AccountOwnershipException extends DomainException {

    private final UUID accountId;
    private final UUID userId;

    public AccountOwnershipException(UUID accountId, UUID userId) {
        super("User " + userId + " does not own account " + accountId);
        this.accountId = accountId;
        this.userId = userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getUserId() {
        return userId;
    }
}
