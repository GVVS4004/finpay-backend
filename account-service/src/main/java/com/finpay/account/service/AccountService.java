package com.finpay.account.service;

import com.finpay.account.domain.Account;
import com.finpay.account.domain.AccountStatus;
import com.finpay.account.domain.ProcessedTransaction;
import com.finpay.account.dto.AccountResponse;
import com.finpay.account.dto.BalanceResponse;
import com.finpay.account.dto.CreateAccountRequest;
import com.finpay.account.exception.AccountFrozenException;
import com.finpay.account.exception.AccountNotFoundException;
import com.finpay.account.exception.InSufficientBalanceException;
import com.finpay.account.repository.AccountRepository;
import com.finpay.account.repository.ProcessedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final ProcessedTransactionRepository processedTransactionRepository;

    public AccountResponse createAccount(Long userId, CreateAccountRequest request){
        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .userId(userId)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(AccountStatus.ACTIVE)
                .build();

        return mapToResponse(accountRepository.save(account));
    }
    public List<AccountResponse> getAccountsByUser(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(String accountNumber) {
        return mapToResponse(findAccountOrThrow(accountNumber));
    }

    @Cacheable(value = "balances", key = "#p0")
    public BalanceResponse getBalance(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);
        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .lastUpdated(account.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "balances", key = "#p0")
    public AccountResponse deposit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null && processedTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Duplicate deposit detected for idempotency key: {}. Skipping.", idempotencyKey);
            return mapToResponse(findAccountOrThrow(accountNumber));
        }

        Account account = findAccountOrThrow(accountNumber);

        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account is frozen: " + accountNumber);
        }

        account.setBalance(account.getBalance().add(amount));
        AccountResponse response = mapToResponse(accountRepository.save(account));

        if (idempotencyKey != null) {
            processedTransactionRepository.save(ProcessedTransaction.builder()
                    .idempotencyKey(idempotencyKey)
                    .accountNumber(accountNumber)
                    .operation("DEPOSIT")
                    .build());
        }

        return response;
    }

    @CacheEvict(value = "balances", key = "#p0")
    public AccountResponse withdraw(String accountNumber, BigDecimal amount) {
        Account account = findAccountOrThrow(accountNumber);

        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account is frozen: " + accountNumber);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InSufficientBalanceException("Insufficient balance in account: " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        return mapToResponse(accountRepository.save(account));
    }

    public AccountResponse updateStatus(String accountNumber, AccountStatus status) {
        Account account = findAccountOrThrow(accountNumber);
        account.setStatus(status);
        return mapToResponse(accountRepository.save(account));
    }

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            number = String.format("%012d", Math.abs(new Random().nextLong() % 1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
