package com.Omnibus.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("Transaction — domain invariants")
class TransactionTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();
    private static final Money AMOUNT = Money.of("500", "USD");

    @Test
    @DisplayName("creates valid transaction with PENDING status")
    void validConstruction() {
        Transaction txn = new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, AMOUNT, "Test");

        assertThat(txn.getId()).isEqualTo(ID);
        assertThat(txn.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(txn.getSourceAccountId()).isEqualTo(SOURCE);
        assertThat(txn.getTargetAccountId()).isEqualTo(TARGET);
        assertThat(txn.getAmount()).isEqualTo(AMOUNT);
        assertThat(txn.getDescription()).isEqualTo("Test");
        assertThat(txn.getCreatedAt()).isNotNull();
        assertThat(txn.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("allows null description")
    void nullDescription() {
        Transaction txn = new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, AMOUNT, null);
        assertThat(txn.getDescription()).isNull();
    }

    @Test
    @DisplayName("rejects null id")
    void nullId() {
        assertThatThrownBy(() -> new Transaction(null, TransactionType.TRANSFER, SOURCE, TARGET, AMOUNT, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    @DisplayName("rejects null type")
    void nullType() {
        assertThatThrownBy(() -> new Transaction(ID, null, SOURCE, TARGET, AMOUNT, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    @DisplayName("rejects null source account")
    void nullSource() {
        assertThatThrownBy(() -> new Transaction(ID, TransactionType.TRANSFER, null, TARGET, AMOUNT, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sourceAccountId");
    }

    @Test
    @DisplayName("rejects null target account")
    void nullTarget() {
        assertThatThrownBy(() -> new Transaction(ID, TransactionType.TRANSFER, SOURCE, null, AMOUNT, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("targetAccountId");
    }

    @Test
    @DisplayName("rejects null amount")
    void nullAmount() {
        assertThatThrownBy(() -> new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, null, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    @DisplayName("rejects zero amount")
    void zeroAmount() {
        assertThatThrownBy(() ->
                new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, Money.zero("USD"), "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("rejects negative amount")
    void negativeAmount() {
        assertThatThrownBy(() ->
                new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, Money.of("-10", "USD"), "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("rejects same source and target account")
    void sameSourceAndTarget() {
        assertThatThrownBy(() ->
                new Transaction(ID, TransactionType.TRANSFER, SOURCE, SOURCE, AMOUNT, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("differ");
    }

    @Test
    @DisplayName("markCompleted sets status and completedAt")
    void markCompleted() {
        Transaction txn = new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, AMOUNT, "x");
        txn.markCompleted();

        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(txn.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("markFailed sets status and completedAt")
    void markFailed() {
        Transaction txn = new Transaction(ID, TransactionType.TRANSFER, SOURCE, TARGET, AMOUNT, "x");
        txn.markFailed();

        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(txn.getCompletedAt()).isNotNull();
    }
}
