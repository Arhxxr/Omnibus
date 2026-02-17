package com.Omnibus.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    List<AccountJpaEntity> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * SELECT ... FOR UPDATE with deterministic ordering (ascending UUID).
     * Prevents deadlocks by always acquiring locks in the same order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id IN :ids ORDER BY a.id")
    List<AccountJpaEntity> findAllByIdForUpdate(@Param("ids") List<UUID> ids);
}
