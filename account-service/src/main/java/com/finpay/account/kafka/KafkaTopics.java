package com.finpay.account.kafka;

public final class KafkaTopics {
    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String PENDING_TRANSACTIONS = "pending-transactions";
    public static final String COMPENSATION_EVENTS = "compensation-events";
    public static final String TRANSACTION_RESULTS = "transaction-results";

    public static final String TRANSACTION_SERVICE_GROUP = "transaction-service";
    public static final String ACCOUNT_SERVICE_GROUP = "account-service";
    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service";
    private KafkaTopics() {}
}