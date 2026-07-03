package com.audittrail.exception;

public class DuplicateFraudFlagException extends AuditTrailException {
    public DuplicateFraudFlagException(String transactionId) {
        super("Transaction '" + transactionId + "' has already been flagged for fraud.");
    }
}
