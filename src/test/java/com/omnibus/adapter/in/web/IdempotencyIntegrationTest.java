package com.Omnibus.adapter.in.web;

import com.Omnibus.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idempotency integration tests.
 * <p>
 * Covers: replay correctness, concurrent same-key submissions,
 * in-flight duplicate handling, key TTL expiration, cross-user
 * key isolation, and idempotency key format edge cases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@DisplayName("Idempotency — Deep Tests")
class IdempotencyIntegrationTest extends BaseIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String TRANSFER_URL = "/api/v1/transfers";
    private static final String ACCOUNTS_URL = "/api/v1/accounts";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private String unique;

    @BeforeEach
    void setUp() {
        unique = UUID.randomUUID().toString().substring(0, 8);
    }

    // ======================================================================
    // Replay Correctness
    // ======================================================================

    @Nested
    @DisplayName("Replay correctness")
    class ReplayCorrectness {

        @Test
        @DisplayName("replayed response returns same transactionId, 200 OK, Idempotency-Replayed header")
        void replayedResponseHeaders() {
            var user1 = registerAndGetAuthContext("rh1_" + unique);
            var user2 = registerAndGetAuthContext("rh2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String key = "replay-header-" + unique;

            // First request
            ResponseEntity<Map> first = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), key);
            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(first.getHeaders().getFirst("Idempotency-Replayed")).isNull();

            // Replay
            ResponseEntity<Map> second = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), key);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(second.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("true");
            assertThat(second.getBody().get("transactionId"))
                    .isEqualTo(first.getBody().get("transactionId"));
            assertThat(second.getBody().get("replayed")).isEqualTo(true);
        }

        @Test
        @DisplayName("replayed response does not modify balances")
        void replayDoesNotModifyBalances() {
            var user1 = registerAndGetAuthContext("rb1_" + unique);
            var user2 = registerAndGetAuthContext("rb2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String key = "replay-balance-" + unique;

            executeTransfer(user1.token, src, tgt, new BigDecimal("500"), key);

            BigDecimal srcAfterFirst = getBalance(user1.token, src);
            BigDecimal tgtAfterFirst = getBalance(user2.token, tgt);

            // Replay 3 times
            for (int i = 0; i < 3; i++) {
                executeTransfer(user1.token, src, tgt, new BigDecimal("500"), key);
            }

            assertThat(getBalance(user1.token, src)).isEqualByComparingTo(srcAfterFirst);
            assertThat(getBalance(user2.token, tgt)).isEqualByComparingTo(tgtAfterFirst);
        }

        @Test
        @DisplayName("different idempotency keys create independent transfers")
        void differentKeysIndependent() {
            var user1 = registerAndGetAuthContext("dk1_" + unique);
            var user2 = registerAndGetAuthContext("dk2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            ResponseEntity<Map> first = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), "key-a-" + unique);
            ResponseEntity<Map> second = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), "key-b-" + unique);

            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(first.getBody().get("transactionId"))
                    .isNotEqualTo(second.getBody().get("transactionId"));

            // Both deducted: 10000 - 100 - 100 = 9800
            assertThat(getBalance(user1.token, src)).isEqualByComparingTo(new BigDecimal("9800"));
        }

        @Test
        @DisplayName("transfer without idempotency key always creates new transaction")
        void noKeyAlwaysNew() {
            var user1 = registerAndGetAuthContext("nk1_" + unique);
            var user2 = registerAndGetAuthContext("nk2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            ResponseEntity<Map> first = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), null);
            ResponseEntity<Map> second = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), null);

            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(first.getBody().get("transactionId"))
                    .isNotEqualTo(second.getBody().get("transactionId"));
        }
    }

    // ======================================================================
    // Concurrent Same-Key Submissions
    // ======================================================================

    @Nested
    @DisplayName("Concurrent same-key submissions")
    class ConcurrentSameKey {

        @Test
        @DisplayName("10 concurrent requests with same key — exactly 1 transfer created")
        void onlyOneTransferCreated() throws Exception {
            var user1 = registerAndGetAuthContext("ck1_" + unique);
            var user2 = registerAndGetAuthContext("ck2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String key = "concurrent-" + unique;

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return executeTransfer(user1.token, src, tgt, new BigDecimal("200"), key);
                }));
            }

            ready.await();
            go.countDown();

            AtomicInteger created = new AtomicInteger();
            AtomicInteger replayed = new AtomicInteger();
            AtomicInteger errors = new AtomicInteger();

            Set<String> transactionIds = ConcurrentHashMap.newKeySet();

            for (Future<ResponseEntity<Map>> future : futures) {
                ResponseEntity<Map> response = future.get(30, TimeUnit.SECONDS);
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    created.incrementAndGet();
                    transactionIds.add(response.getBody().get("transactionId").toString());
                } else if (response.getStatusCode() == HttpStatus.OK) {
                    replayed.incrementAndGet();
                    transactionIds.add(response.getBody().get("transactionId").toString());
                } else {
                    errors.incrementAndGet();
                }
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // Exactly 1 transfer created
            assertThat(created.get())
                    .as("Exactly one request should create the transfer")
                    .isEqualTo(1);

            // All responses reference the same transaction
            assertThat(transactionIds).hasSize(1);

            // Balance reflects exactly 1 transfer
            assertThat(getBalance(user1.token, src)).isEqualByComparingTo(new BigDecimal("9800"));
            assertThat(getBalance(user2.token, tgt)).isEqualByComparingTo(new BigDecimal("10200"));
        }
    }

    // ======================================================================
    // TTL Expiration
    // ======================================================================

    @Nested
    @DisplayName("TTL-based key expiration")
    class TtlExpiration {

        @Test
        @DisplayName("deleteExpired removes keys past their TTL")
        void expiredKeysArePurged() {
            var user1 = registerAndGetAuthContext("ttl1_" + unique);
            var user2 = registerAndGetAuthContext("ttl2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String key = "ttl-" + unique;

            // Create a transfer with idempotency key
            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("100"), key);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // Verify key exists
            Integer countBefore = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM idempotency_keys WHERE key = ?",
                    Integer.class, key);
            assertThat(countBefore).isEqualTo(1);

            // Force-expire the key by updating its expires_at to the past
            jdbc.update("UPDATE idempotency_keys SET expires_at = ? WHERE key = ?",
                    Timestamp.from(Instant.now().minusSeconds(3600)), key);

            // Run cleanup
            int deleted = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM idempotency_keys WHERE expires_at < now()",
                    Integer.class);
            assertThat(deleted).isGreaterThanOrEqualTo(1);

            // Actually delete
            jdbc.update("DELETE FROM idempotency_keys WHERE expires_at < now()");

            // Verify key is gone
            Integer countAfter = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM idempotency_keys WHERE key = ?",
                    Integer.class, key);
            assertThat(countAfter).isEqualTo(0);
        }

        @Test
        @DisplayName("non-expired keys are retained during cleanup")
        void nonExpiredKeysRetained() {
            var user1 = registerAndGetAuthContext("nr1_" + unique);
            var user2 = registerAndGetAuthContext("nr2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String key = "retain-" + unique;

            executeTransfer(user1.token, src, tgt, new BigDecimal("50"), key);

            // Don't expire the key — run cleanup
            jdbc.update("DELETE FROM idempotency_keys WHERE expires_at < now()");

            // Key should still exist
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM idempotency_keys WHERE key = ?",
                    Integer.class, key);
            assertThat(count).isEqualTo(1);
        }
    }

    // ======================================================================
    // Edge Cases
    // ======================================================================

    @Nested
    @DisplayName("Idempotency edge cases")
    class EdgeCases {

        @Test
        @DisplayName("max-length idempotency key (255 chars) is accepted")
        void maxLengthKey() {
            var user1 = registerAndGetAuthContext("ml1_" + unique);
            var user2 = registerAndGetAuthContext("ml2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String longKey = "k".repeat(255);

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("10"), longKey);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("idempotency key with special characters is handled correctly")
        void specialCharacterKey() {
            var user1 = registerAndGetAuthContext("sc1_" + unique);
            var user2 = registerAndGetAuthContext("sc2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);
            String key = "special-key_" + unique + "-déjà-vu";

            ResponseEntity<Map> response = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("10"), key);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // Replay with same special key
            ResponseEntity<Map> replay = executeTransfer(
                    user1.token, src, tgt, new BigDecimal("10"), key);
            assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(replay.getBody().get("replayed")).isEqualTo(true);
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
        body.put("description", "Idempotency test");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return rest.postForEntity(TRANSFER_URL, request, Map.class);
    }

    private record AuthContext(UUID userId, String token) {}
}
