package com.Omnibus.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("LedgerEntry — domain invariants")
class LedgerEntryTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TXN_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Money AMOUNT = Money.of("250", "USD");
    private static final Money BALANCE_AFTER = Money.of("750", "USD");

    @Test
    @DisplayName("creates valid ledger entry")
    void validConstruction() {
        LedgerEntry entry = new LedgerEntry(ID, TXN_ID, ACCOUNT_ID, EntryType.DEBIT, AMOUNT, BALANCE_AFTER);

        assertThat(entry.getId()).isEqualTo(ID);
        assertThat(entry.getTransactionId()).isEqualTo(TXN_ID);
        assertThat(entry.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(entry.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(entry.getAmount()).isEqualTo(AMOUNT);
        assertThat(entry.getBalanceAfter()).isEqualTo(BALANCE_AFTER);
        assertThat(entry.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejects null id")
    void nullId() {
        assertThatThrownBy(() -> new LedgerEntry(null, TXN_ID, ACCOUNT_ID, EntryType.DEBIT, AMOUNT, BALANCE_AFTER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    @DisplayName("rejects null transactionId")
    void nullTransactionId() {
        assertThatThrownBy(() -> new LedgerEntry(ID, null, ACCOUNT_ID, EntryType.DEBIT, AMOUNT, BALANCE_AFTER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionId");
    }

    @Test
    @DisplayName("rejects null accountId")
    void nullAccountId() {
        assertThatThrownBy(() -> new LedgerEntry(ID, TXN_ID, null, EntryType.DEBIT, AMOUNT, BALANCE_AFTER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountId");
    }

    @Test
    @DisplayName("rejects null entryType")
    void nullEntryType() {
        assertThatThrownBy(() -> new LedgerEntry(ID, TXN_ID, ACCOUNT_ID, null, AMOUNT, BALANCE_AFTER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("entryType");
    }

    @Test
    @DisplayName("rejects null amount")
    void nullAmount() {
        assertThatThrownBy(() -> new LedgerEntry(ID, TXN_ID, ACCOUNT_ID, EntryType.DEBIT, null, BALANCE_AFTER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    @DisplayName("rejects null balanceAfter")
    void nullBalanceAfter() {
        assertThatThrownBy(() -> new LedgerEntry(ID, TXN_ID, ACCOUNT_ID, EntryType.DEBIT, AMOUNT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("balanceAfter");
    }
}
