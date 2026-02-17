package com.Omnibus.adapter.in.web;

import com.Omnibus.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the authentication endpoints.
 * <p>
 * Uses a real Spring context + Testcontainers PostgreSQL — no mocks.
 * Every request travels through the full stack: HTTP → Controller →
 * Application Service → Domain → JPA → PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Auth Controller — Integration")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Autowired
    private TestRestTemplate rest;

    /** Unique suffix per test to avoid inter-test collisions. */
    private String unique;

    @BeforeEach
    void setUp() {
        unique = UUID.randomUUID().toString().substring(0, 8);
    }

    // ======================================================================
    // Registration
    // ======================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 with JWT on valid registration")
        void successfulRegistration() {
            var body = registerBody("user_" + unique, "user_" + unique + "@test.com", "P@ssw0rd!!");

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Map<?, ?> json = response.getBody();
            assertThat(json).isNotNull();
            assertThat(json.get("userId")).isNotNull();
            assertThat(json.get("username")).isEqualTo("user_" + unique);
            assertThat(json.get("token")).asString().isNotBlank();
            assertThat(((Number) json.get("expiresInMs")).longValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("returned JWT is a valid 3-part token")
        void registrationTokenIsValidJwt() {
            var body = registerBody("jwt_" + unique, "jwt_" + unique + "@test.com", "P@ssw0rd!!");

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body, Map.class);

            String token = (String) response.getBody().get("token");
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("rejects duplicate username with 400")
        void duplicateUsername() {
            var body = registerBody("dup_" + unique, "dup_" + unique + "@test.com", "P@ssw0rd!!");
            rest.postForEntity(REGISTER_URL, body, Map.class);

            // Same username, different email
            var body2 = registerBody("dup_" + unique, "other_" + unique + "@test.com", "P@ssw0rd!!");
            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body2, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("detail").toString()).contains("Username already taken");
        }

        @Test
        @DisplayName("rejects duplicate email with 400")
        void duplicateEmail() {
            var body = registerBody("emaila_" + unique, "same_" + unique + "@test.com", "P@ssw0rd!!");
            rest.postForEntity(REGISTER_URL, body, Map.class);

            // Different username, same email
            var body2 = registerBody("emailb_" + unique, "same_" + unique + "@test.com", "P@ssw0rd!!");
            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body2, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("detail").toString()).contains("Email already registered");
        }

        @Test
        @DisplayName("rejects blank username with 400 validation error")
        void blankUsername() {
            var body = registerBody("", "blank_" + unique + "@test.com", "P@ssw0rd!!");

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("title")).isEqualTo("Validation Error");
        }

        @Test
        @DisplayName("rejects invalid email with 400 validation error")
        void invalidEmail() {
            var body = registerBody("inv_" + unique, "not-an-email", "P@ssw0rd!!");

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("title")).isEqualTo("Validation Error");
        }

        @Test
        @DisplayName("rejects too-short password with 400 validation error")
        void shortPassword() {
            var body = registerBody("short_" + unique, "short_" + unique + "@test.com", "abc");

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("title")).isEqualTo("Validation Error");
        }
    }

    // ======================================================================
    // Login
    // ======================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with JWT for valid credentials")
        void successfulLogin() {
            // Register first
            rest.postForEntity(REGISTER_URL,
                    registerBody("login_" + unique, "login_" + unique + "@test.com", "P@ssw0rd!!"),
                    Map.class);

            // Login
            var body = loginBody("login_" + unique, "P@ssw0rd!!");
            ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<?, ?> json = response.getBody();
            assertThat(json).isNotNull();
            assertThat(json.get("userId")).isNotNull();
            assertThat(json.get("username")).isEqualTo("login_" + unique);
            assertThat(json.get("token")).asString().isNotBlank();
        }

        @Test
        @DisplayName("rejects unknown username with 400")
        void unknownUser() {
            var body = loginBody("ghost_" + unique, "P@ssw0rd!!");

            ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("detail").toString()).contains("Invalid username or password");
        }

        @Test
        @DisplayName("rejects wrong password with 400 — same message as unknown user")
        void wrongPassword() {
            rest.postForEntity(REGISTER_URL,
                    registerBody("wp_" + unique, "wp_" + unique + "@test.com", "P@ssw0rd!!"),
                    Map.class);

            var body = loginBody("wp_" + unique, "WrongPassword!!");
            ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            // Verify error message does NOT leak which field was wrong
            assertThat(response.getBody().get("detail").toString()).isEqualTo("Invalid username or password");
        }

        @Test
        @DisplayName("rejects blank credentials with 400 validation error")
        void blankCredentials() {
            var body = loginBody("", "");

            ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, body, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ======================================================================
    // JWT Protected Endpoint Integration
    // ======================================================================

    @Nested
    @DisplayName("JWT-protected resources")
    class ProtectedResources {

        @Test
        @DisplayName("accessing /api/v1/accounts without JWT returns 403")
        void unauthenticatedAccessDenied() {
            ResponseEntity<String> response = rest.getForEntity("/api/v1/accounts/me", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("accessing /api/v1/accounts with valid JWT succeeds")
        void authenticatedAccessAllowed() {
            // Register to get a valid JWT
            ResponseEntity<Map> regResponse = rest.postForEntity(REGISTER_URL,
                    registerBody("prot_" + unique, "prot_" + unique + "@test.com", "P@ssw0rd!!"),
                    Map.class);
            String token = (String) regResponse.getBody().get("token");

            // Use the JWT to access a protected endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = rest.exchange(
                    "/api/v1/accounts/me", HttpMethod.GET, request, String.class);

            // Should not be 401/403 — we don't care about the response body for this test,
            // just that the JWT filter accepted the token
            assertThat(response.getStatusCode().value()).isNotIn(401, 403);
        }
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private HttpEntity<Map<String, String>> registerBody(String username, String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of(
                "username", username,
                "email", email,
                "password", password
        ), headers);
    }

    private HttpEntity<Map<String, String>> loginBody(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of(
                "username", username,
                "password", password
        ), headers);
    }
}
