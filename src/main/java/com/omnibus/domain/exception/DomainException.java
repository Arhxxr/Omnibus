package com.Omnibus.domain.exception;

/**
 * Base exception for all domain-level errors.
 * No framework dependencies.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
