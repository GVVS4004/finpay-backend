package com.finpay.transaction.service;

import com.finpay.transaction.client.AccountServiceClient;
import com.finpay.transaction.domain.Transaction;
import com.finpay.transaction.domain.TransactionStatus;
import com.finpay.transaction.domain.TransactionType;
import com.finpay.transaction.dto.AccountResponse;
import com.finpay.transaction.dto.TransactionPageResponse;
import com.finpay.transaction.dto.TransactionResponse;
import com.finpay.transaction.dto.TransferRequest;
import com.finpay.transaction.exception.DuplicateTransactionException;
import com.finpay.transaction.kafka.*;
import com.finpay.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final PaymentEventProducer paymentEventProducer;
    private final PendingTransactionProducer pendingTransactionProducer;
    private final CompensationEventProducer compensationEventProducer;

    private static final BigDecimal SAVINGS_FLAG_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal CURRENT_FLAG_THRESHOLD = new BigDecimal("1000000");

    public TransactionResponse transfer(TransferRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .ifPresent(existing -> {
                        throw new DuplicateTransactionException(idempotencyKey, existing.getTransactionId());
                    });
        }

        // fetch both accounts in parallel — fail fast before touching any money
        CompletableFuture<AccountResponse> fromFuture =
                accountServiceClient.getAccountAsync(request.getFromAccountNumber());
        CompletableFuture<AccountResponse> toFuture =
                accountServiceClient.getAccountAsync(request.getToAccountNumber());

        CompletableFuture.allOf(fromFuture, toFuture).join();

        AccountResponse fromAccount;
        AccountResponse toAccount;
        try {
            fromAccount = fromFuture.get();
            toAccount = toFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch account details: " + e.getMessage());
        }

        if (!"ACTIVE".equals(fromAccount.getStatus()))
            throw new RuntimeException("Source account is not active");
        if (!"ACTIVE".equals(toAccount.getStatus()))
            throw new RuntimeException("Destination account is not active");

        BigDecimal flagThreshold = "CURRENT".equals(fromAccount.getAccountType())
                ? CURRENT_FLAG_THRESHOLD
                : SAVINGS_FLAG_THRESHOLD;

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .flagged(request.getAmount().compareTo(flagThreshold) > 0)
                .description(request.getDescription())
                .build();

        transactionRepository.save(transaction);

        // Step 1: attempt debit via REST
        boolean debitSuccess = false;
        try {
            accountServiceClient.withdraw(request.getFromAccountNumber(), request.getAmount());
            debitSuccess = true;
        } catch (Exception e) {
            log.warn("REST debit failed for transaction {}. Publishing to pending-transactions.",
                    transaction.getTransactionId());
            pendingTransactionProducer.send(new AccountOperationEvent(
                    transaction.getTransactionId(),
                    request.getFromAccountNumber(),
                    request.getAmount(),
                    OperationType.DEBIT,
                    LocalDateTime.now()));
            return mapToResponse(transaction);
        }

        // Step 2: attempt credit via REST
        if (debitSuccess) {
            try {
                accountServiceClient.deposit(request.getToAccountNumber(), request.getAmount(),
                        "credit-" + transaction.getTransactionId());
            } catch (Exception e) {
                log.error("REST credit failed for transaction {}. Attempting saga compensation.",
                        transaction.getTransactionId());

                try {
                    accountServiceClient.deposit(request.getFromAccountNumber(), request.getAmount(),
                            "compensation-" + transaction.getTransactionId());
                    log.info("Saga compensation successful for transaction {}",
                            transaction.getTransactionId());
                } catch (Exception compensationEx) {
                    log.error("CRITICAL: Saga compensation failed for transaction {}. " +
                                    "Publishing to compensation-events. Amount: {} Account: {}",
                            transaction.getTransactionId(), request.getAmount(),
                            request.getFromAccountNumber());
                    // DB write first, then Kafka publish
                    // limitation: not atomic — in production use Debezium CDC
                    compensationEventProducer.send(new AccountOperationEvent(
                            transaction.getTransactionId(),
                            request.getFromAccountNumber(),
                            request.getAmount(),
                            OperationType.COMPENSATE,
                            LocalDateTime.now()));
                }

                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                publishEvent(transaction, TransactionStatus.FAILED);
                throw new RuntimeException("Transfer failed: could not credit destination account");
            }
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        publishEvent(transaction, TransactionStatus.COMPLETED);

        return mapToResponse(transaction);
    }

    public TransactionResponse deposit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .ifPresent(existing -> {
                        throw new DuplicateTransactionException(idempotencyKey, existing.getTransactionId());
                    });
        }

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .toAccountNumber(accountNumber)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);

        try {
            accountServiceClient.deposit(accountNumber, amount,
                    "deposit-" + transaction.getTransactionId());
        } catch (Exception e) {
            log.warn("REST deposit failed for transaction {}. Publishing to pending-transactions.",
                    transaction.getTransactionId());
            pendingTransactionProducer.send(new AccountOperationEvent(
                    transaction.getTransactionId(),
                    accountNumber,
                    amount,
                    OperationType.CREDIT,
                    LocalDateTime.now()));
            return mapToResponse(transaction);
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        publishEvent(transaction, TransactionStatus.COMPLETED);

        return mapToResponse(transaction);
    }

    public TransactionResponse withdraw(String accountNumber, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .ifPresent(existing -> {
                        throw new DuplicateTransactionException(idempotencyKey, existing.getTransactionId());
                    });
        }

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .fromAccountNumber(accountNumber)
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);

        try {
            accountServiceClient.withdraw(accountNumber, amount);
        } catch (Exception e) {
            log.warn("REST withdrawal failed for transaction {}. Publishing to pending-transactions.",
                    transaction.getTransactionId());
            pendingTransactionProducer.send(new AccountOperationEvent(
                    transaction.getTransactionId(),
                    accountNumber,
                    amount,
                    OperationType.DEBIT,
                    LocalDateTime.now()));
            return mapToResponse(transaction);
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        publishEvent(transaction, TransactionStatus.COMPLETED);

        return mapToResponse(transaction);
    }

    public TransactionPageResponse getTransactionsByAccount(String accountNumber, int page, int size) {
        Page<Transaction> transactions = transactionRepository
                .findByFromAccountNumberOrToAccountNumber(
                        accountNumber, accountNumber,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return TransactionPageResponse.builder()
                .content(transactions.getContent().stream().map(this::mapToResponse).toList())
                .page(transactions.getNumber())
                .size(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .build();
    }

    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return mapToResponse(transaction);
    }

    private void publishEvent(Transaction transaction, TransactionStatus status) {
        PaymentEvent event = new PaymentEvent(
                transaction.getTransactionId(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                transaction.getAmount(),
                transaction.getType().name(),
                status.name(),
                LocalDateTime.now()
        );
        paymentEventProducer.send(event);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountNumber(transaction.getToAccountNumber())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
