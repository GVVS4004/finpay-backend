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
public class PaymentEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("taskExecutor")
    public CompletableFuture<Void> send(PaymentEvent event){
        try{
            kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS,event.getTransactionId(),event);
            log.info("[ASYNC] Payment event sent on thread: {} for transaction: {}",
                    Thread.currentThread().getName(), event.getTransactionId());
        }
        catch (Exception e){
            log.error("Failed to send payment event for transaction: {}", event.getTransactionId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
