package com.audittrail.service;

import com.audittrail.dto.AuditSearchRequest;
import com.audittrail.dto.DashboardSummary;
import com.audittrail.dto.FraudFlagRequest;
import com.audittrail.dto.RefundRequest;
import com.audittrail.exception.DuplicateFraudFlagException;
import com.audittrail.model.AuditEvent;
import com.audittrail.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    // Spring injects the repository automatically (constructor injection)
    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditEvent recordRefund(RefundRequest request, String performedBy, String ipAddress) {
        // performedBy is supplied by the controller from the verified identity — never the request
        AuditEvent event = new AuditEvent("REFUND", performedBy);
        event.setAmount(request.getAmount());
        event.setTransactionId(request.getTransactionId());
        event.setMerchantId(request.getMerchantId());
        event.setNotes(request.getNotes());
        event.setIpAddress(ipAddress);
        return repository.save(event);
    }

    public AuditEvent recordFraudFlag(FraudFlagRequest request, String performedBy, String ipAddress) {
        // Business rule: a transaction can't be flagged for fraud twice
        if (repository.existsByTransactionIdAndActionType(request.getTransactionId(), "FRAUD_FLAG")) {
            throw new DuplicateFraudFlagException(request.getTransactionId());
        }
        AuditEvent event = new AuditEvent("FRAUD_FLAG", performedBy);
        event.setTransactionId(request.getTransactionId());
        event.setMerchantId(request.getMerchantId());
        event.setRiskLevel(request.getRiskLevel());
        event.setNotes(request.getNotes());
        event.setIpAddress(ipAddress);
        return repository.save(event);
    }

    public Page<AuditEvent> searchEvents(AuditSearchRequest request) {
        // The service owns the sort order; newest first
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(Sort.Direction.DESC, "performedAt"));
        return repository.searchEvents(
            request.getActionType(),
            request.getMerchantId(),
            request.getPerformedBy(),
            request.getRiskLevel(),
            request.getFromDate(),
            request.getToDate(),
            pageable);
    }

    public DashboardSummary getDashboardSummary() {
        long totalEvents = repository.count();

        long totalRefunds = 0;
        long totalFraudFlags = 0;
        long totalMerchantOnboards = 0;



        for (Object[] row : repository.countByActionType()) {
            String actionType = (String) row[0];
            long count = (Long) row[1];
            switch (actionType) {
                case "REFUND" -> totalRefunds = count;
                case "FRAUD_FLAG" -> totalFraudFlags = count;
                case "MERCHANT_ONBOARD" -> totalMerchantOnboards = count;
                default -> { /* ignore unknown action types */ }
            }
        }

        long highRiskEvents = repository.findHighRiskFraudEvents().size();

        return new DashboardSummary(totalEvents, totalRefunds, totalFraudFlags,
                totalMerchantOnboards, highRiskEvents);
    }

    public List<AuditEvent> getAllEvents() {
        return repository.findAll();
    }

    public List<AuditEvent> getEventsByType(String actionType) {
        return repository.findByActionType(actionType);
    }

    public List<AuditEvent> getHighRiskEvents() {
        return repository.findHighRiskFraudEvents();
    }
}

