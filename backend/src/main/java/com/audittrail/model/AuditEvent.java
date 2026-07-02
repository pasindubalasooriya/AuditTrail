package com.audittrail.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private Instant performedAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private String merchantId;
    private String transactionId;
    private String riskLevel;
    private String notes;
    private String ipAddress;

    // JPA requires a no-argument constructor. 'protected' so our own code
    // uses the meaningful constructor below instead.
    protected AuditEvent() {
    }

    // The constructor our service will use: identity + action are always required,
    // and the server stamps the time itself (never trusts a client-supplied time).
    public AuditEvent(String actionType, String performedBy) {
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.performedAt = Instant.now();
    }

    // ─── Getters and setters ───

    public Long getId() {
        return id;
    }

    public String getActionType() {
        return actionType;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
