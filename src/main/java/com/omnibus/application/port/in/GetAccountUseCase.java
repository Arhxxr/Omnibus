package com.Omnibus.application.port.in;

import com.Omnibus.application.dto.AccountDTO;
import com.Omnibus.application.dto.AccountLookupResponse;
import com.Omnibus.application.dto.TransactionDTO;

import java.util.List;
import java.util.UUID;

/**
 * Use-case port: query account information.
 * All queries are scoped to the authenticated user for authorization.
 */
public interface GetAccountUseCase {

    /**
     * Get an account by ID — verifies the requesting user owns it.
     *
     * @param accountId the account to retrieve
     * @param userId    the authenticated user (for ownership check)
     * @throws com.Omnibus.domain.exception.AccountOwnershipException if the user
     *                                                                   does not
     *                                                                   own the
     *                                                                   account
     */
    AccountDTO getById(UUID accountId, UUID userId);

    List<AccountDTO> getByUserId(UUID userId);

    /**
     * Get transaction history for an account — verifies account ownership.
     *
     * @param accountId the account to get transactions for
     * @param userId    the authenticated user (for ownership check)
     */
    List<TransactionDTO> getTransactionsByAccountId(UUID accountId, UUID userId);

    /**
     * Look up a user's primary account by username for the Send Money wizard.
     * Returns limited info (no balance or sensitive data).
     *
     * @param username the username to look up
     */
    AccountLookupResponse lookupByUsername(String username);
}
