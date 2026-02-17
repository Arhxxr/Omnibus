package com.Omnibus.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a financial account.
 * Balance is stored as Money (BigDecimal + currency).
 */
public class Account {

    private UUID id;
    private UUID userId;
    private String accountNumber;
    private Money balance;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /** JPA / mapper use only. */
    public Account() {
    }

    public Account(UUID id, UUID userId, String accountNumber, Money balance, AccountStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        this.accountNumber = accountNumber;
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Debit (withdraw) the given amount from this account.
     * @return the balance after the debit
     * @throws com.Omnibus.domain.exception.InsufficientFundsException if balance would go negative
     */
    public Money debit(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance.isLessThan(amount)) {
            throw new com.Omnibus.domain.exception.InsufficientFundsException(
                    this.id, this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
        return this.balance;
    }

    /**
     * Credit (deposit) the given amount to this account.
     * @return the balance after the credit
     */
    public Money credit(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
        return this.balance;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    // ---- Getters & Setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Money getBalance() {
        return balance;
    }

    public void setBalance(Money balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
