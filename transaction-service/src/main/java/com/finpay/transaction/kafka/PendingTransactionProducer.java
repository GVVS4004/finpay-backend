package com.finpay.transaction.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingTransactionProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("taskExecutor")
    public CompletableFuture<Void> send(AccountOperationEvent event){
        try{
            kafkaTemplate.send(KafkaTopics.PENDING_TRANSACTIONS,event.getTransactionId(),event);
            log.info("[Async] Pending transaction event sent on thread: {} for transaction: {}",
                    Thread.currentThread().getName(), event.getTransactionId());
        }catch (Exception e){
            log.error("Failed to send pending transaction event for transaction: {}",
                    event.getTransactionId(),e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
