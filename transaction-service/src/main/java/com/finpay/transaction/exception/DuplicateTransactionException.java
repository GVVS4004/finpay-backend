package com.finpay.transaction.exception;

public class DuplicateTransactionException extends RuntimeException {

    private final String existingTransactionId;

    public DuplicateTransactionException(String idempotencyKey, String existingTransactionId) {
        super("Duplicate request detected for idempotency key: " + idempotencyKey +
              ". Existing transaction: " + existingTransactionId);
        this.existingTransactionId = existingTransactionId;
    }

    public String getExistingTransactionId() {
        return existingTransactionId;
    }
}
