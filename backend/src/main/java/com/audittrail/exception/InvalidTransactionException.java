package com.audittrail.exception;

public class InvalidTransactionException extends AuditTrailException {
    public InvalidTransactionException(String transactionId, String reason) {
        super("Invalid transaction '" + transactionId + "': " + reason);
    }
}