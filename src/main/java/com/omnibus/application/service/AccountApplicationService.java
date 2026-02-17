package com.Omnibus.application.service;

import com.Omnibus.application.dto.AccountDTO;
import com.Omnibus.application.dto.AccountLookupResponse;
import com.Omnibus.application.dto.TransactionDTO;
import com.Omnibus.application.port.in.GetAccountUseCase;
import com.Omnibus.application.port.out.AccountRepositoryPort;
import com.Omnibus.application.port.out.TransactionRepositoryPort;
import com.Omnibus.application.port.out.UserRepositoryPort;
import com.Omnibus.domain.exception.AccountOwnershipException;
import com.Omnibus.domain.exception.DomainException;
import com.Omnibus.domain.model.Account;
import com.Omnibus.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountApplicationService implements GetAccountUseCase {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final UserRepositoryPort userRepository;

    public AccountApplicationService(AccountRepositoryPort accountRepository,
            TransactionRepositoryPort transactionRepository,
            UserRepositoryPort userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AccountDTO getById(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new DomainException("Account not found: " + accountId));

        // Authorization: verify requesting user owns this account
        if (!account.getUserId().equals(userId)) {
            throw new AccountOwnershipException(accountId, userId);
        }

        return toDTO(account);
    }

    @Override
    public List<AccountDTO> getByUserId(UUID userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<TransactionDTO> getTransactionsByAccountId(UUID accountId, UUID userId) {
        // Verify account exists and user owns it
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new DomainException("Account not found: " + accountId));

        if (!account.getUserId().equals(userId)) {
            throw new AccountOwnershipException(accountId, userId);
        }

        return transactionRepository.findByAccountId(accountId).stream()
                .map(txn -> new TransactionDTO(
                        txn.getId(),
                        txn.getType().name(),
                        txn.getStatus().name(),
                        txn.getSourceAccountId(),
                        txn.getTargetAccountId(),
                        txn.getAmount().getAmount(),
                        txn.getAmount().getCurrency(),
                        txn.getDescription(),
                        txn.getCreatedAt(),
                        txn.getCompletedAt()))
                .toList();
    }

    @Override
    public AccountLookupResponse lookupByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("User not found: " + username));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        if (accounts.isEmpty()) {
            throw new DomainException("No accounts found for user: " + username);
        }

        // Return the first (primary) account
        Account primary = accounts.get(0);
        return new AccountLookupResponse(
                primary.getId(),
                user.getUsername(),
                primary.getAccountNumber());
    }

    private AccountDTO toDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getBalance().getAmount(),
                account.getBalance().getCurrency(),
                account.getStatus().name(),
                account.getCreatedAt());
    }
}
