package com.finpay.notification.kafka;

import com.finpay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void consume(PaymentEvent event) {
        log.info("Received payment event for transaction: {} status: {}",
                event.getTransactionId(), event.getStatus());
        notificationService.createNotification(event);
    }
}