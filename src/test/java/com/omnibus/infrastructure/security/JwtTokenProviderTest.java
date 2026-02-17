package com.Omnibus.infrastructure.security;

import com.Omnibus.application.port.out.TokenProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link JwtTokenProvider}.
 * Verifies token generation, parsing, validation, and expiry behaviour.
 */
@Tag("unit")
class JwtTokenProviderTest {

    private static final String SECRET =
            "TestSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm!!";
    private static final long EXPIRATION_MS = 900_000; // 15 minutes

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("should implement TokenProviderPort")
    void shouldImplementPort() {
        assertThat(tokenProvider).isInstanceOf(TokenProviderPort.class);
    }

    @Test
    @DisplayName("should generate non-null, non-empty token")
    void shouldGenerateToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        assertThat(token).isNotNull().isNotBlank();
        // JWT has 3 parts separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("should extract correct user ID from token")
    void shouldExtractUserId() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        UUID extracted = tokenProvider.getUserIdFromToken(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    @DisplayName("should extract correct username from token")
    void shouldExtractUsername() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        String username = tokenProvider.getUsernameFromToken(token);

        assertThat(username).isEqualTo("alice");
    }

    @Test
    @DisplayName("should validate a properly signed token")
    void shouldValidateGoodToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("should reject a tampered token")
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        // Tamper with the signature (flip last character)
        String tampered = token.substring(0, token.length() - 1) +
                (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("should reject a garbage string as token")
    void shouldRejectGarbageToken() {
        assertThat(tokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("should reject null token")
    void shouldRejectNullToken() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("should reject empty token")
    void shouldRejectEmptyToken() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("should reject token signed with different secret")
    void shouldRejectWrongSecret() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        // Create a second provider with a different secret
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "DifferentSecretKeyThatIsAlsoAtLeast256BitsLongForHS256!!", EXPIRATION_MS);

        assertThat(otherProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("should reject expired token")
    void shouldRejectExpiredToken() {
        // Create provider with 0ms expiration (instantly expired)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 0);
        UUID userId = UUID.randomUUID();
        String token = expiredProvider.generateToken(userId, "alice");

        // Token should be expired immediately (or within milliseconds)
        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("should return configured expiration milliseconds")
    void shouldReturnExpirationMs() {
        assertThat(tokenProvider.getExpirationMs()).isEqualTo(900_000L);
    }

    @Test
    @DisplayName("should generate unique tokens for different users")
    void shouldGenerateUniqueTokens() {
        String token1 = tokenProvider.generateToken(UUID.randomUUID(), "alice");
        String token2 = tokenProvider.generateToken(UUID.randomUUID(), "bob");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("should produce 3-part JWT structure (header.payload.signature)")
    void shouldProduceStandardJwtStructure() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "alice");

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        // Each part should be non-empty base64url
        for (String part : parts) {
            assertThat(part).isNotBlank();
        }
    }
}
