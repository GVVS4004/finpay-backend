package com.finpay.transaction.client;


import com.finpay.transaction.dto.AccountResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {
    private final RestTemplate restTemplate;

    private static final String ACCOUNT_SERVICE_URL = "http://account-service/api/accounts";

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    public AccountResponse getAccount(String accountNumber){
        return restTemplate.getForObject(
                ACCOUNT_SERVICE_URL + "/{accountNumber}",
                AccountResponse.class,
                accountNumber);
    }

    @Async("taskExecutor")
    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountAsyncFallback")
    public CompletableFuture<AccountResponse> getAccountAsync(String accountNumber) {
        AccountResponse response = restTemplate.getForObject(
                ACCOUNT_SERVICE_URL + "/{accountNumber}",
                AccountResponse.class,
                accountNumber);
        return CompletableFuture.completedFuture(response);
    }

    @CircuitBreaker(name= "accountService", fallbackMethod = "withdrawFallback")
    public void withdraw(String accountNumber, BigDecimal amount){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("amount", amount), headers);
        restTemplate.put(ACCOUNT_SERVICE_URL + "/{accountNumber}/withdraw",
                request, accountNumber);
    }

    @CircuitBreaker(name= "accountService" ,fallbackMethod = "depositFallback")
    public void deposit(String accountNumber, BigDecimal amount, String idempotencyKey){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (idempotencyKey != null) {
            headers.set("X-Idempotency-Key", idempotencyKey);
        }
        HttpEntity<Map<String,Object>> request = new HttpEntity<>(
                Map.of("amount", amount), headers);
        restTemplate.put(ACCOUNT_SERVICE_URL+"/{accountNumber}/deposit", request, accountNumber);
    }

    public AccountResponse getAccountFallback(String accountNumber, Throwable t) {
        log.error("Circuit breaker triggered for getAccount {}: {}", accountNumber, t.getMessage());
        throw new RuntimeException("Account service unavailable. Cannot fetch account details.");
    }

    public CompletableFuture<AccountResponse> getAccountAsyncFallback(String accountNumber, Throwable t) {
        log.error("Circuit breaker triggered for getAccountAsync {}: {}", accountNumber, t.getMessage());
        return CompletableFuture.failedFuture(
                new RuntimeException("Account service unavailable. Cannot fetch account details."));
    }

    public void withdrawFallback(String accountNumber, BigDecimal amount, Throwable t) {
        log.error("Circuit breaker triggered for withdraw on account {}: {}", accountNumber, t.getMessage());
        throw new RuntimeException("Account service unavailable. Please try again later.");
    }

    public void depositFallback(String accountNumber, BigDecimal amount, String idempotencyKey, Throwable t) {
        log.error("Circuit breaker triggered for deposit on account {}: {}", accountNumber, t.getMessage());
        throw new RuntimeException("Account service unavailable. Please try again later.");
    }
}
