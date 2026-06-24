package com.finpay.transaction.kafka;

import com.finpay.transaction.domain.TransactionStatus;
import com.finpay.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionResultConsumer {

    private final TransactionRepository transactionRepository;
//    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = KafkaTopics.TRANSACTION_RESULTS, groupId = KafkaTopics.TRANSACTION_SERVICE_GROUP)
    public void consume(PaymentEvent event) {
        log.info("Received transaction result: {} status: {}", event.getTransactionId(), event.getStatus());

        transactionRepository.findByTransactionId(event.getTransactionId())
                .ifPresentOrElse(transaction -> {
                    TransactionStatus newStatus = TransactionStatus.valueOf(event.getStatus());
                    transaction.setStatus(newStatus);
                    transactionRepository.save(transaction);
                    log.info("Updated transaction {} to status {}", event.getTransactionId(), newStatus);
                }, () -> log.warn("Transaction not found for result event: {}", event.getTransactionId()));
    }
}
