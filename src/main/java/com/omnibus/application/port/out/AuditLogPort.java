package com.Omnibus.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound port for immutable audit logging.
 * Implementations use REQUIRES_NEW propagation so audit entries survive rollbacks.
 */
public interface AuditLogPort {

    void logAccountChange(UUID accountId, UUID actorId, String action,
                          String beforeSnapshot, String afterSnapshot,
                          BigDecimal balanceBefore, BigDecimal balanceAfter);

    void logTransactionEvent(UUID transactionId, UUID actorId, String action,
                             String beforeSnapshot, String afterSnapshot);
}
