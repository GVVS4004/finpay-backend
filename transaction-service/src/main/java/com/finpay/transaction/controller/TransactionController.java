package com.finpay.transaction.controller;

import com.finpay.transaction.dto.TransactionPageResponse;
import com.finpay.transaction.dto.TransactionResponse;
import com.finpay.transaction.dto.TransferRequest;
import com.finpay.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transfer, deposit, withdraw and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer money between accounts")
    public TransactionResponse transfer(
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(request, idempotencyKey);
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Deposit money into an account")
    public TransactionResponse deposit(
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam @NotBlank String accountNumber,
            @RequestParam @NotNull @Positive BigDecimal amount) {
        return transactionService.deposit(accountNumber, amount, idempotencyKey);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Withdraw money from an account")
    public TransactionResponse withdraw(
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam @NotBlank String accountNumber,
            @RequestParam @NotNull @Positive BigDecimal amount) {
        return transactionService.withdraw(accountNumber, amount, idempotencyKey);
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get transaction history for an account")
    public TransactionPageResponse getTransactionsByAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByAccount(accountNumber, page, size);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a transaction by ID")
    public TransactionResponse getTransaction(@PathVariable String transactionId) {
        return transactionService.getTransaction(transactionId);
    }
}