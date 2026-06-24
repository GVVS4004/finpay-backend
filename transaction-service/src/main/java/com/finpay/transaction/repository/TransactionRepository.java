package com.finpay.transaction.repository;

import com.finpay.transaction.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByFromAccountNumberOrToAccountNumber(
            String fromAccountNumber, String toAccountNumber, Pageable pageable);
}