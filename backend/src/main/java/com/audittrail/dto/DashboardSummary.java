package com.audittrail.dto;

public class DashboardSummary {

    // Read-only response object: values set once via the constructor, no setters.
    private final long totalEvents;
    private final long totalRefunds;
    private final long totalFraudFlags;
    private final long totalMerchantOnboards;
    private final long highRiskEvents;

    public DashboardSummary(long totalEvents, long totalRefunds, long totalFraudFlags,
                            long totalMerchantOnboards, long highRiskEvents) {
        this.totalEvents = totalEvents;
        this.totalRefunds = totalRefunds;
        this.totalFraudFlags = totalFraudFlags;
        this.totalMerchantOnboards = totalMerchantOnboards;
        this.highRiskEvents = highRiskEvents;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public long getTotalRefunds() {
        return totalRefunds;
    }

    public long getTotalFraudFlags() {
        return totalFraudFlags;
    }

    public long getTotalMerchantOnboards() {
        return totalMerchantOnboards;
    }

    public long getHighRiskEvents() {
        return highRiskEvents;
    }
}