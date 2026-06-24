package com.finpay.account.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionResultProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendResult(String transactionId, String status) {
        PaymentEvent event = PaymentEvent.builder()
                .transactionId(transactionId)
                .amount(BigDecimal.ZERO)
                .type("TRANSFER")
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send(KafkaTopics.TRANSACTION_RESULTS, transactionId, event);
        log.info("Published transaction result: {} status: {}", transactionId, status);
    }
}
