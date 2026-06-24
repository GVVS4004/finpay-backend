package com.finpay.account.kafka;

import com.finpay.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationConsumer {

    private final AccountService accountService;
    private final TransactionResultProducer transactionResultProducer;

    @KafkaListener(topics = KafkaTopics.COMPENSATION_EVENTS)
    public void consume(AccountOperationEvent event) {
        log.info("Processing compensation for transaction: {}, account: {}, amount: {}",
                event.getTransactionId(), event.getAccountNumber(), event.getAmount());

        // no try-catch — infrastructure exceptions propagate to Spring Kafka for retry
        accountService.deposit(event.getAccountNumber(), event.getAmount(),
                "compensation-" + event.getTransactionId());

        log.info("Compensation successful for transaction {} — money returned to account {}",
                event.getTransactionId(), event.getAccountNumber());

        // transaction still FAILED — compensation just means money was returned
        transactionResultProducer.sendResult(event.getTransactionId(), "FAILED");
    }
}