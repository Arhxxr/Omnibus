package com.Omnibus.domain.exception;

/**
 * Thrown when an account is not in ACTIVE status.
 */
public class AccountNotActiveException extends DomainException {

    public AccountNotActiveException(String message) {
        super(message);
    }
}
