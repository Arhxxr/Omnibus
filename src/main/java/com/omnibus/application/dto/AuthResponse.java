package com.Omnibus.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response returned after successful authentication.
 */
@Schema(description = "Authentication response with JWT token")
public record AuthResponse(
        @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,
        @Schema(description = "Username", example = "jdoe")
        String username,
        @Schema(description = "JWT bearer token")
        String token,
        @Schema(description = "Token TTL in milliseconds", example = "900000")
        long expiresInMs
) {
}
