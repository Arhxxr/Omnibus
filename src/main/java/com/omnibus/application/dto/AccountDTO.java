package com.Omnibus.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for account information.
 */
@Schema(description = "Account details with current balance")
public record AccountDTO(
        @Schema(description = "Account UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Owner user UUID")
        UUID userId,
        @Schema(description = "Human-readable account number", example = "ACC-1708099200000")
        String accountNumber,
        @Schema(description = "Current balance", example = "10000.0000")
        BigDecimal balance,
        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,
        @Schema(description = "Account status", example = "ACTIVE", allowableValues = {"ACTIVE", "FROZEN", "CLOSED"})
        String status,
        @Schema(description = "Account creation timestamp")
        Instant createdAt
) {
}
