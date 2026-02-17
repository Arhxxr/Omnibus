package com.Omnibus.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command object for initiating a transfer.
 */
public record TransferCommand(
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        String description,
        String idempotencyKey,
        UUID actorId
) {
}
