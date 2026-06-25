package com.finpay.transaction.kafka;

import com.finpay.transaction.domain.Transaction;
import com.finpay.transaction.domain.TransactionStatus;
import com.finpay.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionResultConsumer {

    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = KafkaTopics.TRANSACTION_RESULTS, groupId = KafkaTopics.TRANSACTION_SERVICE_GROUP)
    public void consume(PaymentEvent event) {
        log.info("Received transaction result: {} status: {}", event.getTransactionId(), event.getStatus());

        transactionRepository.findByTransactionId(event.getTransactionId())
                .ifPresentOrElse(transaction -> {
                    TransactionStatus newStatus = TransactionStatus.valueOf(event.getStatus());
                    transaction.setStatus(newStatus);
                    transactionRepository.save(transaction);
                    log.info("Updated transaction {} to status {}", event.getTransactionId(), newStatus);
                    publishEvent(transaction, newStatus);
                }, () -> log.warn("Transaction not found for result event: {}", event.getTransactionId()));
    }

    private void publishEvent(Transaction transaction, TransactionStatus status) {
        PaymentEvent outEvent = new PaymentEvent(
                transaction.getTransactionId(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                null,
                null,
                transaction.getAmount(),
                transaction.getType().name(),
                status.name(),
                LocalDateTime.now()
        );
        paymentEventProducer.send(outEvent);
    }
}
