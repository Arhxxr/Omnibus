package com.Omnibus.adapter.out.persistence;

import com.Omnibus.application.port.out.IdempotencyStorePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyPersistenceAdapter implements IdempotencyStorePort {

    private final IdempotencyKeyJpaRepository repository;

    public IdempotencyPersistenceAdapter(IdempotencyKeyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean tryInsert(String key, UUID userId) {
        int inserted = repository.insertIfAbsent(
                key, userId, Instant.now().plus(24, ChronoUnit.HOURS));
        return inserted == 1;
    }

    @Override
    public Optional<CachedResponse> findByKey(String key) {
        return repository.findById(key)
                .filter(entity -> entity.getHttpStatus() != null)
                .map(entity -> new CachedResponse(
                        entity.getHttpStatus(),
                        entity.getResponseBody()
                ));
    }

    @Override
    public void updateResponse(String key, int httpStatus, String responseBody) {
        repository.findById(key).ifPresent(entity -> {
            entity.setHttpStatus(httpStatus);
            entity.setResponseBody(responseBody);
            repository.save(entity);
        });
    }

    @Override
    @Transactional
    public int deleteExpired() {
        return repository.deleteExpired(Instant.now());
    }
}
