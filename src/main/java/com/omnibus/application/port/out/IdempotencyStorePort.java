package com.Omnibus.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for idempotency key storage.
 */
public interface IdempotencyStorePort {

    /**
     * Attempt to claim an idempotency key. Returns true if the key was newly inserted,
     * false if it already existed.
     */
    boolean tryInsert(String key, UUID userId);

    /**
     * Retrieve the cached response for a previously processed idempotency key.
     */
    Optional<CachedResponse> findByKey(String key);

    /**
     * Update the cached response for a key after successful processing.
     */
    void updateResponse(String key, int httpStatus, String responseBody);

    /**
     * Delete expired keys (TTL cleanup).
     */
    int deleteExpired();

    record CachedResponse(int httpStatus, String responseBody) {
    }
}
