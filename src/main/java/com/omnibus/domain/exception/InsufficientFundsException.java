package com.Omnibus.domain.exception;

import com.Omnibus.domain.model.Money;

import java.util.UUID;

/**
 * Thrown when an account does not have sufficient funds for a debit operation.
 */
public class InsufficientFundsException extends DomainException {

    private final UUID accountId;
    private final Money currentBalance;
    private final Money requestedAmount;

    public InsufficientFundsException(UUID accountId, Money currentBalance, Money requestedAmount) {
        super(String.format("Account %s has insufficient funds: balance=%s, requested=%s",
                accountId, currentBalance, requestedAmount));
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Money getCurrentBalance() {
        return currentBalance;
    }

    public Money getRequestedAmount() {
        return requestedAmount;
    }
}
