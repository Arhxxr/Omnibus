package com.Omnibus.adapter.in.web;

import com.Omnibus.application.dto.AccountDTO;
import com.Omnibus.application.dto.AccountLookupResponse;
import com.Omnibus.application.dto.TransactionDTO;
import com.Omnibus.application.port.in.GetAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account queries, transaction history, and recipient lookup")
public class AccountController {

        private final GetAccountUseCase getAccountUseCase;

        public AccountController(GetAccountUseCase getAccountUseCase) {
                this.getAccountUseCase = getAccountUseCase;
        }

        @GetMapping
        @Operation(summary = "List user's accounts", description = "Returns all accounts belonging to the authenticated user.", responses = {
                        @ApiResponse(responseCode = "200", description = "Accounts retrieved")
        })
        public ResponseEntity<List<AccountDTO>> getAccounts(@AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(getAccountUseCase.getByUserId(userId));
        }

        @GetMapping("/{accountId}")
        @Operation(summary = "Get account by ID", description = "Returns a specific account — verifies user ownership.", responses = {
                        @ApiResponse(responseCode = "200", description = "Account retrieved", content = @Content(schema = @Schema(implementation = AccountDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Not the account owner"),
                        @ApiResponse(responseCode = "404", description = "Account not found")
        })
        public ResponseEntity<AccountDTO> getAccount(@PathVariable UUID accountId,
                        @AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(getAccountUseCase.getById(accountId, userId));
        }

        @GetMapping("/{accountId}/transactions")
        @Operation(summary = "Get account transaction history", description = "Returns all transactions where this account is source or target, ordered by date descending.", responses = {
                        @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
                        @ApiResponse(responseCode = "403", description = "Not the account owner"),
                        @ApiResponse(responseCode = "404", description = "Account not found")
        })
        public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable UUID accountId,
                        @AuthenticationPrincipal UUID userId) {
                return ResponseEntity.ok(getAccountUseCase.getTransactionsByAccountId(accountId, userId));
        }

        @GetMapping("/lookup")
        @Operation(summary = "Look up account by username", description = "Returns limited account info (ID, username, account number) for the Send Money wizard. No sensitive data exposed.", responses = {
                        @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = AccountLookupResponse.class))),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<AccountLookupResponse> lookupByUsername(@RequestParam String username) {
                return ResponseEntity.ok(getAccountUseCase.lookupByUsername(username));
        }
}
