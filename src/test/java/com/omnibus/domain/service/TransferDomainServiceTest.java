package com.Omnibus.domain.service;

import com.Omnibus.domain.exception.AccountNotActiveException;
import com.Omnibus.domain.exception.InsufficientFundsException;
import com.Omnibus.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TransferDomainServiceTest {

    private TransferDomainService service;

    @BeforeEach
    void setUp() {
        service = new TransferDomainService();
    }

    private Account createAccount(String balance, AccountStatus status) {
        return new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ACC" + System.nanoTime(),
                Money.of(balance, "USD"),
                status
        );
    }

    private Transaction createTransaction(Account source, Account target, String amount) {
        return new Transaction(
                UUID.randomUUID(),
                TransactionType.TRANSFER,
                source.getId(),
                target.getId(),
                Money.of(amount, "USD"),
                "Test transfer"
        );
    }

    @Test
    void shouldExecuteTransferWithCorrectDoubleEntry() {
        Account source = createAccount("1000", AccountStatus.ACTIVE);
        Account target = createAccount("500", AccountStatus.ACTIVE);
        Transaction txn = createTransaction(source, target, "300");

        TransferDomainService.TransferResult result =
                service.executeTransfer(source, target, Money.of("300", "USD"), txn);

        // Verify balances
        assertEquals(Money.of("700", "USD"), result.sourceBalanceAfter());
        assertEquals(Money.of("800", "USD"), result.targetBalanceAfter());

        // Verify ledger entries
        assertEquals(EntryType.DEBIT, result.debitEntry().getEntryType());
        assertEquals(EntryType.CREDIT, result.creditEntry().getEntryType());

        // Debit and credit amounts must be equal
        assertEquals(result.debitEntry().getAmount(), result.creditEntry().getAmount());
        assertEquals(Money.of("300", "USD"), result.debitEntry().getAmount());

        // Balance-after snapshots
        assertEquals(Money.of("700", "USD"), result.debitEntry().getBalanceAfter());
        assertEquals(Money.of("800", "USD"), result.creditEntry().getBalanceAfter());

        // Transaction status
        assertEquals(TransactionStatus.COMPLETED, txn.getStatus());
        assertNotNull(txn.getCompletedAt());
    }

    @Test
    void shouldRejectTransferWithInsufficientFunds() {
        Account source = createAccount("100", AccountStatus.ACTIVE);
        Account target = createAccount("500", AccountStatus.ACTIVE);
        Transaction txn = createTransaction(source, target, "200");

        assertThrows(InsufficientFundsException.class,
                () -> service.executeTransfer(source, target, Money.of("200", "USD"), txn));

        // Source balance should remain unchanged
        assertEquals(Money.of("100", "USD"), source.getBalance());
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        Account account = createAccount("1000", AccountStatus.ACTIVE);

        // Transaction constructor now enforces source ≠ target invariant
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        UUID.randomUUID(), TransactionType.TRANSFER,
                        account.getId(), account.getId(),
                        Money.of("100", "USD"), "Self transfer"));
    }

    @Test
    void shouldRejectTransferFromInactiveAccount() {
        Account source = createAccount("1000", AccountStatus.FROZEN);
        Account target = createAccount("500", AccountStatus.ACTIVE);
        Transaction txn = createTransaction(source, target, "100");

        assertThrows(AccountNotActiveException.class,
                () -> service.executeTransfer(source, target, Money.of("100", "USD"), txn));
    }

    @Test
    void shouldRejectTransferToInactiveAccount() {
        Account source = createAccount("1000", AccountStatus.ACTIVE);
        Account target = createAccount("500", AccountStatus.CLOSED);
        Transaction txn = createTransaction(source, target, "100");

        assertThrows(AccountNotActiveException.class,
                () -> service.executeTransfer(source, target, Money.of("100", "USD"), txn));
    }

    @Test
    void shouldRejectNegativeAmount() {
        Account source = createAccount("1000", AccountStatus.ACTIVE);
        Account target = createAccount("500", AccountStatus.ACTIVE);
        Transaction txn = createTransaction(source, target, "100");

        assertThrows(IllegalArgumentException.class,
                () -> service.executeTransfer(source, target, Money.of("-100", "USD"), txn));
    }

    @Test
    void shouldRejectZeroAmount() {
        Account source = createAccount("1000", AccountStatus.ACTIVE);
        Account target = createAccount("500", AccountStatus.ACTIVE);
        Transaction txn = createTransaction(source, target, "100");

        assertThrows(IllegalArgumentException.class,
                () -> service.executeTransfer(source, target, Money.of("0", "USD"), txn));
    }
}
