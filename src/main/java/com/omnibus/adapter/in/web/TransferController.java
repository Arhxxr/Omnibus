package com.Omnibus.adapter.in.web;

import com.Omnibus.application.dto.TransferCommand;
import com.Omnibus.application.dto.TransferResult;
import com.Omnibus.application.port.in.CreateTransferUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST adapter for money transfers.
 * All endpoints require JWT authentication.
 * Supports idempotency via the {@code Idempotency-Key} header.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@Validated
@Tag(name = "Transfers")
public class TransferController {

    private final CreateTransferUseCase createTransferUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase) {
        this.createTransferUseCase = createTransferUseCase;
    }

    @PostMapping
    @Operation(summary = "Execute a money transfer",
            description = """
                    Transfers money between two accounts. Supports idempotency via the \
                    `Idempotency-Key` header — replayed requests return the original response \
                    with HTTP 200 and an `Idempotency-Replayed: true` header.
                    
                    Accounts are locked in deterministic UUID order to prevent deadlocks. \
                    Creates atomic DEBIT + CREDIT ledger entries.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer executed successfully",
                    content = @Content(schema = @Schema(implementation = TransferResult.class))),
            @ApiResponse(responseCode = "200", description = "Idempotent replay — original response returned",
                    headers = @Header(name = "Idempotency-Replayed", description = "Set to `true` when replaying a cached response",
                            schema = @Schema(type = "string", example = "true")),
                    content = @Content(schema = @Schema(implementation = TransferResult.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid amount, currency, or account IDs)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "422", description = "Insufficient funds",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransferResult> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            @Size(min = 1, max = 255, message = "Idempotency-Key must be 1-255 characters")
            @Parameter(description = "Client-supplied idempotency key (1-255 chars). Ensures exactly-once processing within 24h TTL.",
                    example = "txn-2026-02-16-001")
            String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {

        TransferCommand command = new TransferCommand(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount(),
                request.currency() != null ? request.currency() : "USD",
                request.description(),
                idempotencyKey,
                userId
        );

        TransferResult result = createTransferUseCase.execute(command);

        HttpHeaders headers = new HttpHeaders();
        if (result.replayed()) {
            headers.set("Idempotency-Replayed", "true");
        }

        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .headers(headers)
                .body(result);
    }

    // ---- Request DTO ----

    @Schema(description = "Transfer request payload")
    public record TransferRequest(
            @Schema(description = "Source account UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotNull UUID sourceAccountId,
            @Schema(description = "Target account UUID", example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
            @NotNull UUID targetAccountId,
            @Schema(description = "Amount to transfer (max 15 integer + 4 decimal digits)", example = "250.00")
            @NotNull @Positive @DecimalMax(value = "999999999999999.9999",
                    message = "Amount must not exceed 999,999,999,999,999.9999")
            BigDecimal amount,
            @Schema(description = "ISO 4217 currency code (3 uppercase letters)", example = "USD", defaultValue = "USD")
            @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
            String currency,
            @Schema(description = "Optional transfer description", example = "Monthly rent payment")
            @Size(max = 500, message = "Description must not exceed 500 characters")
            String description
    ) {}
}
