package com.Omnibus.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Result of a successful transfer operation.
 */
@Schema(description = "Transfer result with updated balances")
public record TransferResult(
        @Schema(description = "Transaction UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID transactionId,
        @Schema(description = "Source account UUID")
        UUID sourceAccountId,
        @Schema(description = "Target account UUID")
        UUID targetAccountId,
        @Schema(description = "Amount transferred", example = "250.0000")
        BigDecimal amount,
        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,
        @Schema(description = "Source account balance after transfer", example = "9750.0000")
        BigDecimal sourceBalanceAfter,
        @Schema(description = "Target account balance after transfer", example = "10250.0000")
        BigDecimal targetBalanceAfter,
        @Schema(description = "Transaction status", example = "COMPLETED")
        String status,
        @Schema(description = "Completion timestamp (ISO 8601)")
        Instant completedAt,
        @Schema(description = "True if this is a replayed idempotent response", example = "false")
        boolean replayed
) {
}
