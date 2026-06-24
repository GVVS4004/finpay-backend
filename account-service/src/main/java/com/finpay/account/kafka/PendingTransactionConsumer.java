package com.finpay.account.kafka;


import com.finpay.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingTransactionConsumer {
    private final AccountService accountService;
    private final TransactionResultProducer transactionResultProducer;

    @KafkaListener(topics = KafkaTopics.PENDING_TRANSACTIONS)
    public void consume(AccountOperationEvent event){
        log.info("Processing pending transaction: {} operation: {}",
                event.getTransactionId(), event.getOperationType());
        try {
            switch (event.getOperationType()) {
                case DEBIT -> accountService.withdraw(event.getAccountNumber(), event.getAmount());
                case CREDIT -> accountService.deposit(event.getAccountNumber(), event.getAmount(),
                        "pending-" + event.getTransactionId());
                default -> log.warn("Unknown operation type: {}", event.getOperationType());
            }
            log.info("Pending transaction {} processed successfully", event.getTransactionId());
            transactionResultProducer.sendResult(event.getTransactionId(), "COMPLETED");
        } catch (Exception e) {
            log.error("Failed to process pending transaction {}: {}", event.getTransactionId(), e.getMessage());
            transactionResultProducer.sendResult(event.getTransactionId(), "FAILED");
        }
    }
}
