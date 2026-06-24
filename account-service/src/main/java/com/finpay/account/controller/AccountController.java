package com.finpay.account.controller;

import com.finpay.account.domain.AccountStatus;
import com.finpay.account.dto.*;
import com.finpay.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management operations")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new account")
    public AccountResponse createAccount(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(userId, request);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all accounts for a user")
    public List<AccountResponse> getAccountsByUser(@PathVariable Long userId) {
        return accountService.getAccountsByUser(userId);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account details")
    public AccountResponse getAccount(@PathVariable String accountNumber) {
        return accountService.getAccount(accountNumber);
    }

    @GetMapping("/{accountNumber}/balance")
    @Operation(summary = "Get account balance")
    public BalanceResponse getBalance(@PathVariable String accountNumber) {
        return accountService.getBalance(accountNumber);
    }

    @PutMapping("/{accountNumber}/deposit")
    @Operation(summary = "Deposit money into account")
    public AccountResponse deposit(
            @PathVariable String accountNumber,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        return accountService.deposit(accountNumber, request.getAmount(), idempotencyKey);
    }

    @PutMapping("/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw money from account")
    public AccountResponse withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest request) {
        return accountService.withdraw(accountNumber, request.getAmount());
    }

    @PutMapping("/{accountNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update account status — admin only")
    public AccountResponse updateStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return accountService.updateStatus(accountNumber, request.getStatus());
    }
}