package com.Omnibus.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Limited account info returned by the lookup endpoint.
 * Does NOT expose balance or other sensitive data.
 */
@Schema(description = "Limited account info for recipient lookup (no sensitive data)")
public record AccountLookupResponse(
        @Schema(description = "Account UUID (used as targetAccountId in transfers)") UUID accountId,
        @Schema(description = "Account owner's username", example = "jdoe") String username,
        @Schema(description = "Human-readable account number", example = "1234567890") String accountNumber) {
}
