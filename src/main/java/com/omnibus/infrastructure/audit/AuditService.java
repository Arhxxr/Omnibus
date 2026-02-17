package com.Omnibus.infrastructure.audit;

import com.Omnibus.adapter.out.persistence.AuditLogJpaEntity;
import com.Omnibus.adapter.out.persistence.AuditLogJpaRepository;
import com.Omnibus.application.port.out.AuditLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Audit log writer with REQUIRES_NEW propagation.
 * Audit entries survive parent transaction rollbacks — failed transfers are still logged.
 */
@Service
public class AuditService implements AuditLogPort {

    private final AuditLogJpaRepository repository;

    public AuditService(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccountChange(UUID accountId, UUID actorId, String action,
                                 String beforeSnapshot, String afterSnapshot,
                                 BigDecimal balanceBefore, BigDecimal balanceAfter) {
        AuditLogJpaEntity log = new AuditLogJpaEntity();
        log.setEntityType("ACCOUNT");
        log.setEntityId(accountId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setBeforeSnapshot(beforeSnapshot);
        log.setAfterSnapshot(afterSnapshot);
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceAfter);
        repository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransactionEvent(UUID transactionId, UUID actorId, String action,
                                    String beforeSnapshot, String afterSnapshot) {
        AuditLogJpaEntity log = new AuditLogJpaEntity();
        log.setEntityType("TRANSACTION");
        log.setEntityId(transactionId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setBeforeSnapshot(beforeSnapshot);
        log.setAfterSnapshot(afterSnapshot);
        repository.save(log);
    }
}
