package com.Omnibus.application.service;

import com.Omnibus.application.dto.TransferCommand;
import com.Omnibus.application.dto.TransferResult;
import com.Omnibus.application.port.in.CreateTransferUseCase;
import com.Omnibus.application.port.out.*;
import com.Omnibus.domain.exception.AccountOwnershipException;
import com.Omnibus.domain.exception.DomainException;
import com.Omnibus.domain.model.*;
import com.Omnibus.domain.service.TransferDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Orchestrates the transfer use case:
 * 1. Check idempotency key
 * 2. Lock accounts in deterministic order (ascending UUID)
 * 3. Execute double-entry bookkeeping via domain service
 * 4. Persist ledger entries + update account balances
 * 5. Write audit log (REQUIRES_NEW — survives rollback)
 * 6. Cache response in idempotency store
 */
@Service
public class TransferApplicationService implements CreateTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferApplicationService.class);

    private final TransferDomainService transferDomainService;
    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final LedgerRepositoryPort ledgerRepository;
    private final IdempotencyStorePort idempotencyStore;
    private final AuditLogPort auditLog;

    public TransferApplicationService(TransferDomainService transferDomainService,
                                      AccountRepositoryPort accountRepository,
                                      TransactionRepositoryPort transactionRepository,
                                      LedgerRepositoryPort ledgerRepository,
                                      IdempotencyStorePort idempotencyStore,
                                      AuditLogPort auditLog) {
        this.transferDomainService = transferDomainService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyStore = idempotencyStore;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public TransferResult execute(TransferCommand command) {
        log.info("Processing transfer: {} -> {}, amount={} {}",
                command.sourceAccountId(), command.targetAccountId(),
                command.amount(), command.currency());

        // ---- Step 1: Idempotency Check ----
        if (command.idempotencyKey() != null) {
            boolean isNew = idempotencyStore.tryInsert(command.idempotencyKey(), command.actorId());
            if (!isNew) {
                // Duplicate request — attempt to return cached response
                log.info("Duplicate idempotency key detected: {}", command.idempotencyKey());
                return handleIdempotencyReplay(command.idempotencyKey());
            }
        }

        // ---- Step 2: Lock accounts in deterministic order (ascending UUID) ----
        List<UUID> sortedIds = new ArrayList<>(List.of(
                command.sourceAccountId(), command.targetAccountId()));
        sortedIds.sort(UUID::compareTo);

        List<Account> lockedAccounts = accountRepository.findAllByIdForUpdate(sortedIds);
        if (lockedAccounts.size() != 2) {
            throw new DomainException("One or both accounts not found");
        }

        // Map back to source/target
        Account source = lockedAccounts.stream()
                .filter(a -> a.getId().equals(command.sourceAccountId()))
                .findFirst()
                .orElseThrow(() -> new DomainException("Source account not found: " + command.sourceAccountId()));

        Account target = lockedAccounts.stream()
                .filter(a -> a.getId().equals(command.targetAccountId()))
                .findFirst()
                .orElseThrow(() -> new DomainException("Target account not found: " + command.targetAccountId()));

        // ---- Authorization: verify the requesting user owns the source account ----
        if (!source.getUserId().equals(command.actorId())) {
            throw new AccountOwnershipException(source.getId(), command.actorId());
        }

        // Capture before-state for audit
        var sourceBalanceBefore = source.getBalance().getAmount();
        var targetBalanceBefore = target.getBalance().getAmount();

        // ---- Step 3: Create transaction record ----
        Money transferAmount = Money.of(command.amount(), command.currency());
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                TransactionType.TRANSFER,
                command.sourceAccountId(),
                command.targetAccountId(),
                transferAmount,
                command.description()
        );
        transaction.setIdempotencyKey(command.idempotencyKey());

        // ---- Step 4: Execute domain logic (double-entry) ----
        TransferDomainService.TransferResult domainResult;
        try {
            domainResult = transferDomainService.executeTransfer(source, target, transferAmount, transaction);
        } catch (Exception e) {
            transaction.markFailed();
            transactionRepository.save(transaction);

            // Audit the failure (REQUIRES_NEW — survives this rollback)
            auditLog.logTransactionEvent(transaction.getId(), command.actorId(), "TRANSFER_FAILED",
                    null, "{\"error\": \"" + e.getMessage() + "\"}");

            throw e;
        }

        // ---- Step 5: Persist everything ----
        transactionRepository.save(transaction);
        ledgerRepository.save(domainResult.debitEntry());
        ledgerRepository.save(domainResult.creditEntry());
        accountRepository.save(source);
        accountRepository.save(target);

        // ---- Step 6: Audit log (REQUIRES_NEW) ----
        auditLog.logAccountChange(
                source.getId(), command.actorId(), "DEBIT",
                "{\"balance\": " + sourceBalanceBefore + "}",
                "{\"balance\": " + domainResult.sourceBalanceAfter().getAmount() + "}",
                sourceBalanceBefore,
                domainResult.sourceBalanceAfter().getAmount()
        );

        auditLog.logAccountChange(
                target.getId(), command.actorId(), "CREDIT",
                "{\"balance\": " + targetBalanceBefore + "}",
                "{\"balance\": " + domainResult.targetBalanceAfter().getAmount() + "}",
                targetBalanceBefore,
                domainResult.targetBalanceAfter().getAmount()
        );

        // ---- Step 7: Build result ----
        TransferResult result = new TransferResult(
                transaction.getId(),
                command.sourceAccountId(),
                command.targetAccountId(),
                command.amount(),
                command.currency(),
                domainResult.sourceBalanceAfter().getAmount(),
                domainResult.targetBalanceAfter().getAmount(),
                transaction.getStatus().name(),
                transaction.getCompletedAt(),
                false
        );

        // Update idempotency cache
        if (command.idempotencyKey() != null) {
            idempotencyStore.updateResponse(command.idempotencyKey(), 200,
                    serializeResult(result));
        }

        log.info("Transfer completed: txnId={}, sourceBalance={}, targetBalance={}",
                transaction.getId(),
                domainResult.sourceBalanceAfter(),
                domainResult.targetBalanceAfter());

        return result;
    }

    private TransferResult handleIdempotencyReplay(String idempotencyKey) {
        var cached = idempotencyStore.findByKey(idempotencyKey);
        if (cached.isPresent()) {
            // Return cached result as replayed
            var txn = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new DomainException("Idempotency key found but transaction missing"));
            return new TransferResult(
                    txn.getId(),
                    txn.getSourceAccountId(),
                    txn.getTargetAccountId(),
                    txn.getAmount().getAmount(),
                    txn.getAmount().getCurrency(),
                    null, null,
                    txn.getStatus().name(),
                    txn.getCompletedAt(),
                    true  // replayed
            );
        }
        // Key exists but response not yet cached — the first request might still be in-flight
        // This happens due to the same-transaction semantics. Throw conflict.
        throw new DomainException("Request with this idempotency key is currently being processed");
    }

    private String serializeResult(TransferResult result) {
        return String.format(
                "{\"transactionId\":\"%s\",\"amount\":%s,\"status\":\"%s\"}",
                result.transactionId(), result.amount(), result.status()
        );
    }
}
