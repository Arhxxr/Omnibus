package com.Omnibus.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyKeyJpaEntity i WHERE i.expiresAt < :now")
    int deleteExpired(Instant now);

    @Modifying
    @Query(value = "INSERT INTO idempotency_keys (key, user_id, expires_at, created_at) " +
            "VALUES (:key, :userId, :expiresAt, now()) ON CONFLICT (key) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(String key, UUID userId, Instant expiresAt);
}
