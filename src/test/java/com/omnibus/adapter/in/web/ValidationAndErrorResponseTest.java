package com.Omnibus.adapter.in.web;

import com.Omnibus.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for input validation hardening and standardized error responses.
 * <p>
 * Validates: RFC 7807 ProblemDetail format, validation constraints
 * on transfer requests, malformed body handling, edge-case amounts,
 * and error consistency across all controller endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@DisplayName("Validation & Error Responses")
class ValidationAndErrorResponseTest extends BaseIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String TRANSFER_URL = "/api/v1/transfers";
    private static final String ACCOUNTS_URL = "/api/v1/accounts";

    @Autowired
    private TestRestTemplate rest;

    private String unique;

    @BeforeEach
    void setUp() {
        unique = UUID.randomUUID().toString().substring(0, 8);
    }

    // ======================================================================
    // Transfer Validation
    // ======================================================================

    @Nested
    @DisplayName("Transfer input validation")
    class TransferValidation {

        @Test
        @DisplayName("null sourceAccountId → 400 with validation error")
        void nullSourceAccountId() {
            var user = registerAndGetAuthContext("nsrc_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("targetAccountId", UUID.randomUUID().toString());
            body.put("amount", 100);

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertProblemDetail(response.getBody(), "Validation Error");
        }

        @Test
        @DisplayName("null targetAccountId → 400 with validation error")
        void nullTargetAccountId() {
            var user = registerAndGetAuthContext("ntgt_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sourceAccountId", UUID.randomUUID().toString());
            body.put("amount", 100);

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertProblemDetail(response.getBody(), "Validation Error");
        }

        @Test
        @DisplayName("null amount → 400 with validation error")
        void nullAmount() {
            var user = registerAndGetAuthContext("namt_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sourceAccountId", UUID.randomUUID().toString());
            body.put("targetAccountId", UUID.randomUUID().toString());

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("zero amount → 400 (not positive)")
        void zeroAmount() {
            var user = registerAndGetAuthContext("za_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = transferBody(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO);

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("negative amount → 400")
        void negativeAmount() {
            var user = registerAndGetAuthContext("neg_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = transferBody(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-100"));

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("invalid currency code (lowercase) → 400")
        void lowercaseCurrency() {
            var user = registerAndGetAuthContext("lc_" + unique);
            var user2 = registerAndGetAuthContext("lc2_" + unique);

            UUID src = getFirstAccountId(user.token);
            UUID tgt = getFirstAccountId(user2.token);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sourceAccountId", src.toString());
            body.put("targetAccountId", tgt.toString());
            body.put("amount", 100);
            body.put("currency", "usd"); // lowercase — pattern requires ^[A-Z]{3}$

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("invalid currency code (4 chars) → 400")
        void fourCharCurrency() {
            var user = registerAndGetAuthContext("4c_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sourceAccountId", UUID.randomUUID().toString());
            body.put("targetAccountId", UUID.randomUUID().toString());
            body.put("amount", 100);
            body.put("currency", "USDT");

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("description exceeding 500 chars → 400")
        void descriptionTooLong() {
            var user = registerAndGetAuthContext("dl_" + unique);

            HttpHeaders headers = authHeaders(user.token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sourceAccountId", UUID.randomUUID().toString());
            body.put("targetAccountId", UUID.randomUUID().toString());
            body.put("amount", 100);
            body.put("description", "x".repeat(501));

            ResponseEntity<Map> response = postTransfer(headers, body);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("same source and target account → 400")
        void sameAccount() {
            var user = registerAndGetAuthContext("sa_" + unique);
            UUID accountId = getFirstAccountId(user.token);

            ResponseEntity<Map> response = executeTransfer(
                    user.token, accountId, accountId, new BigDecimal("100"), null);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ======================================================================
    // Malformed Request Bodies
    // ======================================================================

    @Nested
    @DisplayName("Malformed request handling")
    class MalformedRequests {

        @Test
        @DisplayName("empty body → 400")
        void emptyBody() {
            var user = registerAndGetAuthContext("eb_" + unique);
            HttpHeaders headers = authHeaders(user.token);

            HttpEntity<String> request = new HttpEntity<>("{}", headers);
            ResponseEntity<Map> response = rest.postForEntity(TRANSFER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("invalid JSON → 400 with 'Bad Request' title")
        void invalidJson() {
            var user = registerAndGetAuthContext("ij_" + unique);
            HttpHeaders headers = authHeaders(user.token);

            HttpEntity<String> request = new HttpEntity<>("{not valid json}", headers);
            ResponseEntity<Map> response = rest.postForEntity(TRANSFER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("amount as string → 400")
        void amountAsString() {
            var user = registerAndGetAuthContext("as_" + unique);
            HttpHeaders headers = authHeaders(user.token);

            String jsonBody = String.format("""
                    {"sourceAccountId":"%s","targetAccountId":"%s","amount":"not-a-number"}""",
                    UUID.randomUUID(), UUID.randomUUID());

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<Map> response = rest.postForEntity(TRANSFER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("malformed UUID → 400")
        void malformedUuid() {
            var user = registerAndGetAuthContext("mu_" + unique);
            HttpHeaders headers = authHeaders(user.token);

            String jsonBody = """
                    {"sourceAccountId":"not-a-uuid","targetAccountId":"also-not","amount":100}""";

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<Map> response = rest.postForEntity(TRANSFER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ======================================================================
    // RFC 7807 ProblemDetail Format
    // ======================================================================

    @Nested
    @DisplayName("RFC 7807 error response format")
    class ProblemDetailFormat {

        @Test
        @DisplayName("insufficient funds error follows ProblemDetail spec")
        void insufficientFundsProblemDetail() {
            var user1 = registerAndGetAuthContext("pd1_" + unique);
            var user2 = registerAndGetAuthContext("pd2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("99999"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            Map<?, ?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("title")).isEqualTo("Insufficient Funds");
            assertThat(body.get("status")).isEqualTo(422);
            assertThat(body.get("detail")).asString().contains("insufficient funds");
            assertThat(body.get("type")).asString().contains("insufficient-funds");
            assertThat(body.get("timestamp")).isNotNull();
            assertThat(body.get("accountId")).isNotNull();
        }

        @Test
        @DisplayName("domain error follows ProblemDetail spec")
        void domainErrorProblemDetail() {
            var user = registerAndGetAuthContext("de_" + unique);

            UUID src = getFirstAccountId(user.token);
            UUID fake = UUID.randomUUID();

            ResponseEntity<Map> response = executeTransfer(
                    user.token, src, fake, new BigDecimal("100"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            Map<?, ?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("title")).isEqualTo("Business Rule Violation");
            assertThat(body.get("status")).isEqualTo(400);
            assertThat(body.get("timestamp")).isNotNull();
        }

        @Test
        @DisplayName("validation error includes field-level errors list")
        void validationErrorWithFieldErrors() {
            var user = registerAndGetAuthContext("ve_" + unique);
            HttpHeaders headers = authHeaders(user.token);

            // Missing all required fields
            HttpEntity<String> request = new HttpEntity<>("{}", headers);
            ResponseEntity<Map> response = rest.postForEntity(TRANSFER_URL, request, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            Map<?, ?> body = response.getBody();
            assertThat(body.get("title")).isEqualTo("Validation Error");
            assertThat(body.get("errors")).isNotNull();
            assertThat(body.get("errors")).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) body.get("errors");
            assertThat(errors).hasSizeGreaterThanOrEqualTo(3); // at least source, target, amount
        }
    }

    // ======================================================================
    // Auth Validation
    // ======================================================================

    @Nested
    @DisplayName("Auth input validation")
    class AuthValidation {

        @Test
        @DisplayName("register with empty username → 400")
        void emptyUsername() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("username", "", "email", "test@test.com", "password", "P@ssw0rd!!"),
                    headers);

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("register with invalid email → 400")
        void invalidEmail() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("username", "validuser_" + unique,
                            "email", "not-an-email",
                            "password", "P@ssw0rd!!"),
                    headers);

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("register with short password → 400")
        void shortPassword() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("username", "shortpw_" + unique,
                            "email", "shortpw_" + unique + "@test.com",
                            "password", "short"),
                    headers);

            ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("login with empty body → 400")
        void loginEmptyBody() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            ResponseEntity<Map> response = rest.postForEntity(LOGIN_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ======================================================================
    // Account Endpoint Validation
    // ======================================================================

    @Nested
    @DisplayName("Account endpoint validation")
    class AccountValidation {

        @Test
        @DisplayName("GET /accounts without auth → 403")
        void accountsNoAuth() {
            ResponseEntity<String> response = rest.getForEntity(ACCOUNTS_URL, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("GET /accounts/{id} with invalid UUID → 400")
        void accountsInvalidUuid() {
            var user = registerAndGetAuthContext("aiuuid_" + unique);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(user.token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = rest.exchange(
                    ACCOUNTS_URL + "/not-a-uuid", HttpMethod.GET, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("GET /accounts/{id} for another user's account → 403 (IDOR prevention)")
        void cannotAccessOtherUsersAccount() {
            var user1 = registerAndGetAuthContext("idor1_" + unique);
            var user2 = registerAndGetAuthContext("idor2_" + unique);

            UUID user2Account = getFirstAccountId(user2.token);

            // User 1 tries to access User 2's account
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(user1.token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = rest.exchange(
                    ACCOUNTS_URL + "/" + user2Account, HttpMethod.GET, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("title")).isEqualTo("Access Denied");
        }

        @Test
        @DisplayName("POST /transfers from another user's source account → 403")
        void cannotTransferFromOtherUsersAccount() {
            var user1 = registerAndGetAuthContext("idor_src1_" + unique);
            var user2 = registerAndGetAuthContext("idor_src2_" + unique);

            UUID user1Account = getFirstAccountId(user1.token);
            UUID user2Account = getFirstAccountId(user2.token);

            // User 2 tries to transfer FROM User 1's account
            HttpHeaders headers = authHeaders(user2.token);
            headers.set("Idempotency-Key", "idor-transfer-" + unique);

            Map<String, Object> body = transferBody(user1Account, user2Account, new BigDecimal("100"));
            ResponseEntity<Map> response = postTransfer(headers, body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("title")).isEqualTo("Access Denied");
        }

        @Test
        @DisplayName("GET /accounts/{id} by account owner → 200")
        void ownerCanAccessOwnAccount() {
            var user = registerAndGetAuthContext("owner_" + unique);
            UUID accountId = getFirstAccountId(user.token);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(user.token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = rest.exchange(
                    ACCOUNTS_URL + "/" + accountId, HttpMethod.GET, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("id")).isEqualTo(accountId.toString());
        }
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private AuthContext registerAndGetAuthContext(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("username", username,
                        "email", username + "@test.com",
                        "password", "P@ssw0rd!!"),
                headers);

        ResponseEntity<Map> response = rest.postForEntity(REGISTER_URL, request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<?, ?> body = response.getBody();
        return new AuthContext(
                UUID.fromString(body.get("userId").toString()),
                body.get("token").toString()
        );
    }

    private UUID getFirstAccountId(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> response = rest.exchange(
                ACCOUNTS_URL, HttpMethod.GET, request, List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) response.getBody().get(0);
        return UUID.fromString(account.get("id").toString());
    }

    private BigDecimal getBalance(String token, UUID accountId) {
        return new BigDecimal(getAccount(token, accountId).get("balance").toString());
    }

    private Map<?, ?> getAccount(String token, UUID accountId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = rest.exchange(
                ACCOUNTS_URL + "/" + accountId, HttpMethod.GET, request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private Map<String, Object> transferBody(UUID src, UUID tgt, BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceAccountId", src.toString());
        body.put("targetAccountId", tgt.toString());
        body.put("amount", amount);
        body.put("currency", "USD");
        return body;
    }

    private ResponseEntity<Map> postTransfer(HttpHeaders headers, Map<String, Object> body) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return rest.postForEntity(TRANSFER_URL, request, Map.class);
    }

    private ResponseEntity<Map> executeTransfer(String token, UUID sourceAccountId,
                                                 UUID targetAccountId, BigDecimal amount,
                                                 String idempotencyKey) {
        HttpHeaders headers = authHeaders(token);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceAccountId", sourceAccountId.toString());
        body.put("targetAccountId", targetAccountId.toString());
        body.put("amount", amount);
        body.put("currency", "USD");
        body.put("description", "Validation test transfer");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return rest.postForEntity(TRANSFER_URL, request, Map.class);
    }

    private void assertProblemDetail(Map<?, ?> body, String expectedTitle) {
        assertThat(body).isNotNull();
        assertThat(body.get("title")).isEqualTo(expectedTitle);
        assertThat(body.get("status")).isNotNull();
        assertThat(body.get("timestamp")).isNotNull();
    }

    private record AuthContext(UUID userId, String token) {}
}
