package com.audittrail.service;

import com.audittrail.dto.AuditSearchRequest;
import com.audittrail.dto.DashboardSummary;
import com.audittrail.dto.FraudFlagRequest;
import com.audittrail.dto.RefundRequest;
import com.audittrail.exception.DuplicateFraudFlagException;
import com.audittrail.model.AuditEvent;
import com.audittrail.repository.AuditEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
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

        // Build the WHERE clause dynamically: add a condition ONLY for the filters
        // that were actually provided. No conditions = match everything. This avoids
        // the ":param IS NULL OR ..." pattern that Postgres can't type-infer.
        Specification<AuditEvent> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (request.getActionType() != null) {
                filters.add(cb.equal(root.get("actionType"), request.getActionType()));
            }
            if (request.getMerchantId() != null) {
                filters.add(cb.equal(root.get("merchantId"), request.getMerchantId()));
            }
            if (request.getPerformedBy() != null) {
                filters.add(cb.equal(root.get("performedBy"), request.getPerformedBy()));
            }
            if (request.getRiskLevel() != null) {
                filters.add(cb.equal(root.get("riskLevel"), request.getRiskLevel()));
            }
            if (request.getFromDate() != null) {
                filters.add(cb.greaterThanOrEqualTo(root.<Instant>get("performedAt"), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                filters.add(cb.lessThanOrEqualTo(root.<Instant>get("performedAt"), request.getToDate()));
            }
            return cb.and(filters.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable);
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

