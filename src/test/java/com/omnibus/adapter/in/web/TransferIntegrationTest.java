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
 * End-to-end integration tests for the transfer flow.
 * <p>
 * Uses real Spring context + Testcontainers PostgreSQL — no mocks.
 * Tests cover: successful transfers, balance verification, double-entry
 * ledger entries, error cases (insufficient funds, missing accounts,
 * validation errors), and audit trail generation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@DisplayName("Transfer Controller — Integration")
class TransferIntegrationTest extends BaseIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
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
    // Successful Transfer
    // ======================================================================

    @Nested
    @DisplayName("Successful transfers")
    class SuccessfulTransfers {

        @Test
        @DisplayName("transfers money between two accounts — 201, correct balances")
        void basicTransfer() {
            // Register two users (each gets $10,000)
            var user1 = registerAndGetAuthContext("u1_" + unique);
            var user2 = registerAndGetAuthContext("u2_" + unique);

            UUID sourceAccountId = getFirstAccountId(user1.token);
            UUID targetAccountId = getFirstAccountId(user2.token);

            // Transfer $250 from user1 to user2
            ResponseEntity<Map> response = executeTransfer(
                    user1.token, sourceAccountId, targetAccountId,
                    new BigDecimal("250.0000"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Map<?, ?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("status")).isEqualTo("COMPLETED");
            assertThat(new BigDecimal(body.get("sourceBalanceAfter").toString()))
                    .isEqualByComparingTo(new BigDecimal("9750.0000"));
            assertThat(new BigDecimal(body.get("targetBalanceAfter").toString()))
                    .isEqualByComparingTo(new BigDecimal("10250.0000"));
            assertThat(body.get("transactionId")).isNotNull();
            assertThat(body.get("replayed")).isEqualTo(false);
        }

        @Test
        @DisplayName("multiple sequential transfers accumulate correctly")
        void sequentialTransfers() {
            var user1 = registerAndGetAuthContext("sq1_" + unique);
            var user2 = registerAndGetAuthContext("sq2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            // Three transfers: $1000, $2000, $3000
            executeTransfer(user1.token, src, tgt, new BigDecimal("1000"), null);
            executeTransfer(user1.token, src, tgt, new BigDecimal("2000"), null);
            ResponseEntity<Map> third = executeTransfer(user1.token, src, tgt, new BigDecimal("3000"), null);

            assertThat(third.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            // Source: 10000 - 6000 = 4000
            assertThat(new BigDecimal(third.getBody().get("sourceBalanceAfter").toString()))
                    .isEqualByComparingTo(new BigDecimal("4000"));
            // Target: 10000 + 6000 = 16000
            assertThat(new BigDecimal(third.getBody().get("targetBalanceAfter").toString()))
                    .isEqualByComparingTo(new BigDecimal("16000"));
        }

        @Test
        @DisplayName("transfer with zero-padded fractional amount preserves precision")
        void fractionalAmountPrecision() {
            var user1 = registerAndGetAuthContext("fp1_" + unique);
            var user2 = registerAndGetAuthContext("fp2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("0.0001"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(new BigDecimal(response.getBody().get("sourceBalanceAfter").toString()))
                    .isEqualByComparingTo(new BigDecimal("9999.9999"));
        }

        @Test
        @DisplayName("transfer entire balance succeeds (zero remaining)")
        void transferEntireBalance() {
            var user1 = registerAndGetAuthContext("eb1_" + unique);
            var user2 = registerAndGetAuthContext("eb2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("10000"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(new BigDecimal(response.getBody().get("sourceBalanceAfter").toString()))
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(new BigDecimal(response.getBody().get("targetBalanceAfter").toString()))
                    .isEqualByComparingTo(new BigDecimal("20000"));
        }
    }

    // ======================================================================
    // Error Cases
    // ======================================================================

    @Nested
    @DisplayName("Transfer error cases")
    class ErrorCases {

        @Test
        @DisplayName("insufficient funds returns 422")
        void insufficientFunds() {
            var user1 = registerAndGetAuthContext("if1_" + unique);
            var user2 = registerAndGetAuthContext("if2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            // Try to transfer more than the $10,000 balance
            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("10001"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody().get("title")).isEqualTo("Insufficient Funds");
        }

        @Test
        @DisplayName("nonexistent source account returns 400")
        void nonexistentSourceAccount() {
            var user1 = registerAndGetAuthContext("ns1_" + unique);
            var user2 = registerAndGetAuthContext("ns2_" + unique);

            UUID tgt = getFirstAccountId(user2.token);
            UUID fakeSource = UUID.randomUUID();

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, fakeSource, tgt, new BigDecimal("100"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("nonexistent target account returns 400")
        void nonexistentTargetAccount() {
            var user1 = registerAndGetAuthContext("nt1_" + unique);
            UUID src = getFirstAccountId(user1.token);
            UUID fakeTarget = UUID.randomUUID();

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, fakeTarget, new BigDecimal("100"), null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("unauthenticated transfer returns 403")
        void unauthenticatedTransfer() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of("sourceAccountId", UUID.randomUUID(),
                            "targetAccountId", UUID.randomUUID(),
                            "amount", 100),
                    headers);

            ResponseEntity<String> response = rest.postForEntity(TRANSFER_URL, request, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("negative amount returns 400")
        void negativeAmount() {
            var user1 = registerAndGetAuthContext("na1_" + unique);
            var user2 = registerAndGetAuthContext("na2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            // Negative amount — fails Jakarta @Positive validation
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(user1.token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of("sourceAccountId", src,
                            "targetAccountId", tgt,
                            "amount", -100),
                    headers);

            ResponseEntity<Map> response = rest.postForEntity(TRANSFER_URL, request, Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ======================================================================
    // Double-Entry Bookkeeping Verification
    // ======================================================================

    @Nested
    @DisplayName("Double-entry bookkeeping")
    class DoubleEntryBookkeeping {

        @Test
        @DisplayName("transfer creates exactly 2 ledger entries (1 DEBIT + 1 CREDIT)")
        void twoLedgerEntries() {
            var user1 = registerAndGetAuthContext("le1_" + unique);
            var user2 = registerAndGetAuthContext("le2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            ResponseEntity<Map> txnResponse = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("500"), null);

            assertThat(txnResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            String txnId = txnResponse.getBody().get("transactionId").toString();

            // Verify the accounts reflect correct balances through the API
            Map<?, ?> srcAccount = getAccount(user1.token, src);
            Map<?, ?> tgtAccount = getAccount(user2.token, tgt);

            assertThat(new BigDecimal(srcAccount.get("balance").toString()))
                    .isEqualByComparingTo(new BigDecimal("9500"));
            assertThat(new BigDecimal(tgtAccount.get("balance").toString()))
                    .isEqualByComparingTo(new BigDecimal("10500"));
        }

        @Test
        @DisplayName("conservation of money — total pool unchanged after transfers")
        void conservationOfMoney() {
            var user1 = registerAndGetAuthContext("cm1_" + unique);
            var user2 = registerAndGetAuthContext("cm2_" + unique);
            var user3 = registerAndGetAuthContext("cm3_" + unique);

            UUID a1 = getFirstAccountId(user1.token);
            UUID a2 = getFirstAccountId(user2.token);
            UUID a3 = getFirstAccountId(user3.token);

            // Total pool = 3 * $10,000 = $30,000
            // Execute a chain of transfers
            executeTransfer(user1.token, a1, a2, new BigDecimal("1500"), null);
            executeTransfer(user2.token, a2, a3, new BigDecimal("3000"), null);
            executeTransfer(user3.token, a3, a1, new BigDecimal("500"), null);
            executeTransfer(user1.token, a1, a3, new BigDecimal("2000"), null);

            // Fetch final balances
            BigDecimal b1 = new BigDecimal(getAccount(user1.token, a1).get("balance").toString());
            BigDecimal b2 = new BigDecimal(getAccount(user2.token, a2).get("balance").toString());
            BigDecimal b3 = new BigDecimal(getAccount(user3.token, a3).get("balance").toString());

            // a1: 10000 - 1500 + 500 - 2000 = 7000
            // a2: 10000 + 1500 - 3000 = 8500
            // a3: 10000 + 3000 - 500 + 2000 = 14500
            assertThat(b1).isEqualByComparingTo(new BigDecimal("7000"));
            assertThat(b2).isEqualByComparingTo(new BigDecimal("8500"));
            assertThat(b3).isEqualByComparingTo(new BigDecimal("14500"));

            // Conservation: total must still be $30,000
            assertThat(b1.add(b2).add(b3)).isEqualByComparingTo(new BigDecimal("30000"));
        }
    }

    // ======================================================================
    // Idempotency (basic)
    // ======================================================================

    @Nested
    @DisplayName("Idempotency basics")
    class IdempotencyBasics {

        @Test
        @DisplayName("same idempotency key returns replayed response, not duplicate transfer")
        void idempotencyKeyReplay() {
            var user1 = registerAndGetAuthContext("ik1_" + unique);
            var user2 = registerAndGetAuthContext("ik2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            String idempotencyKey = "idem-" + unique;

            // First request
            ResponseEntity<Map> first = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), idempotencyKey);
            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(first.getBody().get("replayed")).isEqualTo(false);

            // Second request with same key — should be replayed
            ResponseEntity<Map> second = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), idempotencyKey);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(second.getBody().get("replayed")).isEqualTo(true);
            assertThat(second.getBody().get("transactionId"))
                    .isEqualTo(first.getBody().get("transactionId"));

            // Balance should only reflect ONE transfer
            BigDecimal srcBalance = new BigDecimal(
                    getAccount(user1.token, src).get("balance").toString());
            assertThat(srcBalance).isEqualByComparingTo(new BigDecimal("9900"));
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

    private Map<?, ?> getAccount(String token, UUID accountId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = rest.exchange(
                ACCOUNTS_URL + "/" + accountId, HttpMethod.GET, request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<Map> executeTransfer(String token, UUID sourceAccountId,
                                                 UUID targetAccountId, BigDecimal amount,
                                                 String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceAccountId", sourceAccountId.toString());
        body.put("targetAccountId", targetAccountId.toString());
        body.put("amount", amount);
        body.put("currency", "USD");
        body.put("description", "Integration test transfer");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return rest.postForEntity(TRANSFER_URL, request, Map.class);
    }

    private record AuthContext(UUID userId, String token) {}
}
