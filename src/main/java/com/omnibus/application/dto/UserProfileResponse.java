package com.Omnibus.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Response for the /auth/me endpoint — current user profile with accounts.
 */
@Schema(description = "Current user profile with associated accounts")
public record UserProfileResponse(
        @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000") UUID userId,
        @Schema(description = "Username", example = "jdoe") String username,
        @Schema(description = "Email address", example = "jdoe@example.com") String email,
        @Schema(description = "User's accounts with current balances") List<AccountDTO> accounts) {
}
