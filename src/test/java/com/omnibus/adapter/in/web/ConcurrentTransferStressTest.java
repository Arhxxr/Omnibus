package com.Omnibus.adapter.in.web;

import com.Omnibus.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent transfer stress tests.
 * <p>
 * Validates that pessimistic locking ({@code SELECT ... FOR UPDATE}) prevents
 * lost updates, that deterministic UUID ordering prevents deadlocks, and that
 * the double-entry bookkeeping invariant holds under concurrent load.
 * <p>
 * All tests use real PostgreSQL via Testcontainers — no mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@Tag("concurrency")
@DisplayName("Concurrent Transfer Stress Tests")
class ConcurrentTransferStressTest extends BaseIntegrationTest {

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
    // Pessimistic Locking — No Lost Updates
    // ======================================================================

    @Nested
    @DisplayName("Pessimistic locking verification")
    class PessimisticLocking {

        @Test
        @DisplayName("20 concurrent transfers from same source — no lost updates, final balance exact")
        void noLostUpdatesUnderConcurrency() throws Exception {
            // Setup: user1 has $10,000, user2 has $10,000
            var user1 = registerAndGetAuthContext("pl1_" + unique);
            var user2 = registerAndGetAuthContext("pl2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            int threadCount = 20;
            BigDecimal amountPerTransfer = new BigDecimal("100"); // 20 × $100 = $2,000

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await(); // All threads start simultaneously
                    ResponseEntity<Map> response = executeTransfer(
                            user1.token, src, tgt, amountPerTransfer, null);
                    return response.getStatusCode();
                }));
            }

            ready.await();
            go.countDown(); // Fire!

            AtomicInteger successes = new AtomicInteger();
            AtomicInteger failures = new AtomicInteger();

            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(30, TimeUnit.SECONDS);
                if (status.is2xxSuccessful()) {
                    successes.incrementAndGet();
                } else {
                    failures.incrementAndGet();
                }
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // All 20 should succeed (no lost updates thanks to pessimistic locking)
            assertThat(successes.get()).isEqualTo(threadCount);

            // Verify exact final balances
            BigDecimal srcBalance = getBalance(user1.token, src);
            BigDecimal tgtBalance = getBalance(user2.token, tgt);

            // Source: 10,000 - (20 × 100) = 8,000
            assertThat(srcBalance).isEqualByComparingTo(new BigDecimal("8000"));
            // Target: 10,000 + (20 × 100) = 12,000
            assertThat(tgtBalance).isEqualByComparingTo(new BigDecimal("12000"));

            // Conservation: total still $20,000
            assertThat(srcBalance.add(tgtBalance)).isEqualByComparingTo(new BigDecimal("20000"));
        }

        @Test
        @DisplayName("concurrent transfers drain account — exactly the right ones fail once balance exhausted")
        void concurrentDrainAccount() throws Exception {
            var user1 = registerAndGetAuthContext("cd1_" + unique);
            var user2 = registerAndGetAuthContext("cd2_" + unique);

            UUID src = getFirstAccountId(user1.token);
            UUID tgt = getFirstAccountId(user2.token);

            // 15 threads try to transfer $1,000 each, but source only has $10,000
            // Exactly 10 should succeed, 5 should fail with 422 (insufficient funds)
            int threadCount = 15;
            BigDecimal amountPerTransfer = new BigDecimal("1000");

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    ResponseEntity<Map> response = executeTransfer(
                            user1.token, src, tgt, amountPerTransfer, null);
                    return response.getStatusCode();
                }));
            }

            ready.await();
            go.countDown();

            AtomicInteger ok = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            AtomicInteger other = new AtomicInteger();

            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(30, TimeUnit.SECONDS);
                if (status.is2xxSuccessful()) {
                    ok.incrementAndGet();
                } else if (status.value() == 422) {
                    rejected.incrementAndGet();
                } else {
                    other.incrementAndGet();
                }
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            assertThat(other.get())
                    .as("No unexpected errors (500s)")
                    .isEqualTo(0);

            assertThat(ok.get())
                    .as("Exactly 10 transfers succeed ($10,000 ÷ $1,000)")
                    .isEqualTo(10);

            assertThat(rejected.get())
                    .as("Exactly 5 transfers rejected (insufficient funds)")
                    .isEqualTo(5);

            // Source should be exactly $0
            BigDecimal srcBalance = getBalance(user1.token, src);
            assertThat(srcBalance).isEqualByComparingTo(BigDecimal.ZERO);

            // Target should be exactly $20,000 (own $10k + received $10k)
            BigDecimal tgtBalance = getBalance(user2.token, tgt);
            assertThat(tgtBalance).isEqualByComparingTo(new BigDecimal("20000"));
        }
    }

    // ======================================================================
    // Deadlock Prevention — Deterministic Lock Ordering
    // ======================================================================

    @Nested
    @DisplayName("Deadlock prevention via deterministic UUID ordering")
    class DeadlockPrevention {

        @Test
        @DisplayName("bidirectional concurrent transfers — no deadlocks, conservation of money")
        void bidirectionalTransfersNoDeadlock() throws Exception {
            // Two users transfer money back and forth simultaneously.
            // Without deterministic lock ordering, this would deadlock:
            //   Thread A: LOCK(acctA) → LOCK(acctB)
            //   Thread B: LOCK(acctB) → LOCK(acctA) → DEADLOCK!
            // Our implementation sorts UUIDs ascending before locking.

            var user1 = registerAndGetAuthContext("dl1_" + unique);
            var user2 = registerAndGetAuthContext("dl2_" + unique);

            UUID acctA = getFirstAccountId(user1.token);
            UUID acctB = getFirstAccountId(user2.token);

            int roundTrips = 10; // 10 transfers each direction = 20 total
            BigDecimal amount = new BigDecimal("50");

            ExecutorService executor = Executors.newFixedThreadPool(roundTrips * 2);
            CountDownLatch ready = new CountDownLatch(roundTrips * 2);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();

            // Transfers from A → B
            for (int i = 0; i < roundTrips; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return executeTransfer(user1.token, acctA, acctB, amount, null).getStatusCode();
                }));
            }

            // Transfers from B → A
            for (int i = 0; i < roundTrips; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return executeTransfer(user2.token, acctB, acctA, amount, null).getStatusCode();
                }));
            }

            ready.await();
            go.countDown();

            AtomicInteger successes = new AtomicInteger();
            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(30, TimeUnit.SECONDS);
                if (status.is2xxSuccessful()) successes.incrementAndGet();
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // All transfers should succeed — no deadlocks
            assertThat(successes.get()).isEqualTo(roundTrips * 2);

            // Net effect: A sends 10×$50=$500 to B, B sends 10×$50=$500 to A → balances unchanged
            BigDecimal balA = getBalance(user1.token, acctA);
            BigDecimal balB = getBalance(user2.token, acctB);
            assertThat(balA).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(balB).isEqualByComparingTo(new BigDecimal("10000"));

            // Conservation: total still $20,000
            assertThat(balA.add(balB)).isEqualByComparingTo(new BigDecimal("20000"));
        }

        @Test
        @DisplayName("multi-account fan-out — 4 accounts, concurrent cross-transfers, zero-sum")
        void multiAccountFanOutNoDeadlock() throws Exception {
            // 4 users, each transfers to every other simultaneously
            var users = new ArrayList<AuthContext>();
            var accounts = new ArrayList<UUID>();

            for (int i = 0; i < 4; i++) {
                var user = registerAndGetAuthContext("mu" + i + "_" + unique);
                users.add(user);
                accounts.add(getFirstAccountId(user.token));
            }

            BigDecimal amount = new BigDecimal("100");
            BigDecimal totalBefore = new BigDecimal("40000"); // 4 × $10,000

            ExecutorService executor = Executors.newFixedThreadPool(12);
            CountDownLatch ready = new CountDownLatch(12);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();

            // Each user i transfers $100 to every other user j
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (i == j) continue;
                    final int fi = i, fj = j;
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        go.await();
                        return executeTransfer(
                                users.get(fi).token, accounts.get(fi), accounts.get(fj),
                                amount, null).getStatusCode();
                    }));
                }
            }

            ready.await();
            go.countDown();

            AtomicInteger successes = new AtomicInteger();
            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(30, TimeUnit.SECONDS);
                if (status.is2xxSuccessful()) successes.incrementAndGet();
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // All 12 transfers should succeed (no deadlocks)
            assertThat(successes.get()).isEqualTo(12);

            // Each account: sent 3×$100=$300, received 3×$100=$300 → balance unchanged
            BigDecimal totalAfter = BigDecimal.ZERO;
            for (int i = 0; i < 4; i++) {
                BigDecimal bal = getBalance(users.get(i).token, accounts.get(i));
                assertThat(bal)
                        .as("Account %d balance should be $10,000 (sent $300 and received $300)", i)
                        .isEqualByComparingTo(new BigDecimal("10000"));
                totalAfter = totalAfter.add(bal);
            }

            // Conservation: total still $40,000
            assertThat(totalAfter).isEqualByComparingTo(totalBefore);
        }
    }

    // ======================================================================
    // Double-Entry Invariants Under Concurrency
    // ======================================================================

    @Nested
    @DisplayName("Double-entry invariants under concurrency")
    class DoubleEntryInvariants {

        @Test
        @DisplayName("global ledger: sum of all DEBIT entries == sum of all CREDIT entries")
        void globalDebitEqualsCredit() throws Exception {
            // Run several concurrent transfers then verify the global invariant
            var user1 = registerAndGetAuthContext("gi1_" + unique);
            var user2 = registerAndGetAuthContext("gi2_" + unique);
            var user3 = registerAndGetAuthContext("gi3_" + unique);

            UUID a1 = getFirstAccountId(user1.token);
            UUID a2 = getFirstAccountId(user2.token);
            UUID a3 = getFirstAccountId(user3.token);

            ExecutorService executor = Executors.newFixedThreadPool(6);
            CountDownLatch ready = new CountDownLatch(6);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();

            // 6 transfers:
            // u1→u2 $500, u2→u3 $300, u3→u1 $200
            // u2→u1 $150, u1→u3 $100, u3→u2 $50
            Object[][] transfers = {
                    {user1, a1, a2, new BigDecimal("500")},
                    {user2, a2, a3, new BigDecimal("300")},
                    {user3, a3, a1, new BigDecimal("200")},
                    {user2, a2, a1, new BigDecimal("150")},
                    {user1, a1, a3, new BigDecimal("100")},
                    {user3, a3, a2, new BigDecimal("50")},
            };

            for (Object[] t : transfers) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    AuthContext user = (AuthContext) t[0];
                    return executeTransfer(user.token, (UUID) t[1], (UUID) t[2],
                            (BigDecimal) t[3], null).getStatusCode();
                }));
            }

            ready.await();
            go.countDown();

            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(30, TimeUnit.SECONDS);
                assertThat(status.is2xxSuccessful())
                        .as("All transfers should succeed")
                        .isTrue();
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // Verify global invariant: sum(DEBIT amounts) == sum(CREDIT amounts)
            BigDecimal totalDebits = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE entry_type = 'DEBIT'",
                    BigDecimal.class);
            BigDecimal totalCredits = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE entry_type = 'CREDIT'",
                    BigDecimal.class);

            assertThat(totalDebits)
                    .as("Sum of all DEBIT ledger entries must equal sum of all CREDIT entries")
                    .isEqualByComparingTo(totalCredits);

            // Each completed transaction should have exactly 2 ledger entries
            Integer txnCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM transactions WHERE status = 'COMPLETED'",
                    Integer.class);
            Integer entryCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ledger_entries",
                    Integer.class);

            assertThat(entryCount)
                    .as("Each transaction should produce exactly 2 ledger entries")
                    .isEqualTo(txnCount * 2);
        }

        @Test
        @DisplayName("conservation of money: sum of all account balances never changes")
        void conservationOfMoney() throws Exception {
            // 3 users, $10k each = $30k total
            var user1 = registerAndGetAuthContext("cm1_" + unique);
            var user2 = registerAndGetAuthContext("cm2_" + unique);
            var user3 = registerAndGetAuthContext("cm3_" + unique);

            UUID a1 = getFirstAccountId(user1.token);
            UUID a2 = getFirstAccountId(user2.token);
            UUID a3 = getFirstAccountId(user3.token);

            BigDecimal totalBefore = getBalance(user1.token, a1)
                    .add(getBalance(user2.token, a2))
                    .add(getBalance(user3.token, a3));
            assertThat(totalBefore).isEqualByComparingTo(new BigDecimal("30000"));

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch ready = new CountDownLatch(10);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();

            // 10 random-ish transfers between the 3 accounts
            BigDecimal[] amounts = {
                    new BigDecimal("100"), new BigDecimal("250"), new BigDecimal("50"),
                    new BigDecimal("300"), new BigDecimal("175"), new BigDecimal("80"),
                    new BigDecimal("400"), new BigDecimal("60"), new BigDecimal("500"),
                    new BigDecimal("150"),
            };
            Object[][] routes = {
                    {user1, a1, a2}, {user2, a2, a3}, {user3, a3, a1},
                    {user1, a1, a3}, {user2, a2, a1}, {user3, a3, a2},
                    {user1, a1, a2}, {user2, a2, a3}, {user3, a3, a1},
                    {user1, a1, a3},
            };

            for (int i = 0; i < 10; i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    AuthContext user = (AuthContext) routes[idx][0];
                    return executeTransfer(user.token, (UUID) routes[idx][1],
                            (UUID) routes[idx][2], amounts[idx], null).getStatusCode();
                }));
            }

            ready.await();
            go.countDown();

            for (Future<HttpStatusCode> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // Verify conservation — total must still be $30,000
            BigDecimal totalAfter = getBalance(user1.token, a1)
                    .add(getBalance(user2.token, a2))
                    .add(getBalance(user3.token, a3));

            assertThat(totalAfter)
                    .as("Total money across all accounts must be conserved")
                    .isEqualByComparingTo(new BigDecimal("30000"));

            // Also verify via direct DB query
            BigDecimal dbTotal = jdbc.queryForObject(
                    "SELECT SUM(balance) FROM accounts", BigDecimal.class);
            assertThat(dbTotal)
                    .as("DB-level sum of all balances must equal $30,000 + other test accounts")
                    .isGreaterThanOrEqualTo(new BigDecimal("30000"));
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
        Map<?, ?> account = getAccount(token, accountId);
        return new BigDecimal(account.get("balance").toString());
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
        body.put("description", "Concurrent stress test transfer");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return rest.postForEntity(TRANSFER_URL, request, Map.class);
    }

    private record AuthContext(UUID userId, String token) {}
}
