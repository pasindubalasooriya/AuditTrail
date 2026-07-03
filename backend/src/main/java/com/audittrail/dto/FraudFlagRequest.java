package com.audittrail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class FraudFlagRequest {

    @NotBlank
    private String transactionId;

    private String merchantId;

    @NotBlank
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
             message = "riskLevel must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    private String riskLevel;

    private String notes;

    public FraudFlagRequest() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}