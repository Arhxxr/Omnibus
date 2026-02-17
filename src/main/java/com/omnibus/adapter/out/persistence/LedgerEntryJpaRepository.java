package com.Omnibus.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    List<LedgerEntryJpaEntity> findByTransactionId(UUID transactionId);

    List<LedgerEntryJpaEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
