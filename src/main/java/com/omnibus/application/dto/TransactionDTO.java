package com.Omnibus.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for transaction information returned in account history.
 */
@Schema(description = "Transaction details for account history")
public record TransactionDTO(
        @Schema(description = "Transaction UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
        @Schema(description = "Transaction type", example = "TRANSFER", allowableValues = {
                "TRANSFER", "DEPOSIT", "WITHDRAWAL", "FEE" }) String type,
        @Schema(description = "Transaction status", example = "COMPLETED", allowableValues = { "PENDING", "COMPLETED",
                "FAILED" }) String status,
        @Schema(description = "Source account UUID") UUID sourceAccountId,
        @Schema(description = "Target account UUID") UUID targetAccountId,
        @Schema(description = "Transaction amount", example = "250.0000") BigDecimal amount,
        @Schema(description = "ISO 4217 currency code", example = "USD") String currency,
        @Schema(description = "Optional transaction description", example = "Monthly rent payment") String description,
        @Schema(description = "Transaction creation timestamp") Instant createdAt,
        @Schema(description = "Transaction completion timestamp (null if pending)") Instant completedAt){
}
