package com.finpay.notification.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String type;
    private String status;
    private LocalDateTime timestamp;
}