package com.Omnibus.application.port.out;

/**
 * Outbound port for password encoding/matching.
 * Decouples application layer from Spring Security.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
