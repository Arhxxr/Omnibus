package com.Omnibus.application.port.out;

import java.util.UUID;

/**
 * Outbound port for JWT token operations.
 * Decouples the application layer from the infrastructure JWT implementation.
 */
public interface TokenProviderPort {

    String generateToken(UUID userId, String username);

    UUID getUserIdFromToken(String token);

    String getUsernameFromToken(String token);

    boolean validateToken(String token);

    long getExpirationMs();
}
