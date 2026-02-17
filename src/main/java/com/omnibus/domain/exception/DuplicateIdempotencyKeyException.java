package com.Omnibus.domain.exception;

/**
 * Thrown when a duplicate idempotency key is detected and the cached response is returned.
 */
public class DuplicateIdempotencyKeyException extends DomainException {

    private final String idempotencyKey;

    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("Duplicate idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
