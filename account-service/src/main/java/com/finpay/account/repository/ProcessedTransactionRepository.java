package com.finpay.account.repository;

import com.finpay.account.domain.ProcessedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}