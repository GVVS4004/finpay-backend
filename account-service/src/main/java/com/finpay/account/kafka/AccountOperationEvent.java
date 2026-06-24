package com.finpay.account.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountOperationEvent {
    private String transactionId;
    private String accountNumber;
    private BigDecimal amount;
    private OperationType operationType;
    private LocalDateTime timestamp;
}