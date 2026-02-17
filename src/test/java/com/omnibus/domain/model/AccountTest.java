package com.Omnibus.domain.model;

import com.Omnibus.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Account — domain invariants")
class AccountTest {

    private Account createAccount(String balance) {
        return new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1234567890",
                Money.of(balance, "USD"),
                AccountStatus.ACTIVE
        );
    }

    // ---- Constructor guard clauses ----

    @Test
    @DisplayName("rejects null id")
    void nullId() {
        assertThatThrownBy(() -> new Account(null, UUID.randomUUID(), "123", Money.zero("USD"), AccountStatus.ACTIVE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    @DisplayName("rejects null userId")
    void nullUserId() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), null, "123", Money.zero("USD"), AccountStatus.ACTIVE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("rejects blank accountNumber")
    void blankAccountNumber() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), UUID.randomUUID(), " ", Money.zero("USD"), AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountNumber");
    }

    @Test
    @DisplayName("rejects null balance")
    void nullBalance() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), UUID.randomUUID(), "123", null, AccountStatus.ACTIVE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("balance");
    }

    @Test
    @DisplayName("rejects null status")
    void nullStatus() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), UUID.randomUUID(), "123", Money.zero("USD"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }

    // ---- Debit / Credit ----

    @Test
    void shouldDebitSuccessfully() {
        Account account = createAccount("1000");
        Money result = account.debit(Money.of("300", "USD"));
        assertEquals(Money.of("700", "USD"), result);
        assertEquals(Money.of("700", "USD"), account.getBalance());
    }

    @Test
    void shouldCreditSuccessfully() {
        Account account = createAccount("1000");
        Money result = account.credit(Money.of("500", "USD"));
        assertEquals(Money.of("1500", "USD"), result);
        assertEquals(Money.of("1500", "USD"), account.getBalance());
    }

    @Test
    void shouldThrowOnInsufficientFunds() {
        Account account = createAccount("100");
        assertThrows(InsufficientFundsException.class,
                () -> account.debit(Money.of("200", "USD")));
        // Balance should remain unchanged
        assertEquals(Money.of("100", "USD"), account.getBalance());
    }

    @Test
    void shouldDebitExactBalance() {
        Account account = createAccount("100");
        Money result = account.debit(Money.of("100", "USD"));
        assertEquals(Money.of("0", "USD"), result);
    }

    @Test
    void shouldRejectNegativeDebit() {
        Account account = createAccount("100");
        assertThrows(IllegalArgumentException.class,
                () -> account.debit(Money.of("-50", "USD")));
    }

    @Test
    void shouldRejectNegativeCredit() {
        Account account = createAccount("100");
        assertThrows(IllegalArgumentException.class,
                () -> account.credit(Money.of("-50", "USD")));
    }

    @Test
    void shouldRejectZeroDebit() {
        Account account = createAccount("100");
        assertThrows(IllegalArgumentException.class,
                () -> account.debit(Money.of("0", "USD")));
    }

    @Test
    void isActiveShouldReflectStatus() {
        Account active = createAccount("100");
        assertTrue(active.isActive());

        active.setStatus(AccountStatus.FROZEN);
        assertFalse(active.isActive());
    }
}
