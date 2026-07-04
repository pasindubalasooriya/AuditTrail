package com.audittrail.controller;

import com.audittrail.config.JwtUtils;
import com.audittrail.dto.AuditSearchRequest;
import com.audittrail.dto.DashboardSummary;
import com.audittrail.dto.FraudFlagRequest;
import com.audittrail.dto.RefundRequest;
import com.audittrail.model.AuditEvent;
import com.audittrail.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final JwtUtils jwtUtils;

    public AuditController(AuditService auditService, JwtUtils jwtUtils) {
        this.auditService = auditService;
        this.jwtUtils = jwtUtils;
    }

    // Investigative + reporting roles may record refunds
    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST','COMPLIANCE_OFFICER')")
    public ResponseEntity<AuditEvent> recordRefund(
            @Valid @RequestBody RefundRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String performedBy = jwtUtils.getUsername(authentication);
        AuditEvent saved = auditService.recordRefund(request, performedBy, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Flagging fraud is an investigative action — analysts only
    @PostMapping("/fraud-flag")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    public ResponseEntity<AuditEvent> recordFraudFlag(
            @Valid @RequestBody FraudFlagRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String performedBy = jwtUtils.getUsername(authentication);
        AuditEvent saved = auditService.recordFraudFlag(request, performedBy, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Shared operational reads
    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST','COMPLIANCE_OFFICER')")
    public ResponseEntity<List<AuditEvent>> getAllEvents() {
        return ResponseEntity.ok(auditService.getAllEvents());
    }

    // Compliance reporting surface — officers only
    @GetMapping("/events/by-type")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<AuditEvent>> getEventsByType(@RequestParam String type) {
        return ResponseEntity.ok(auditService.getEventsByType(type));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST','COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<AuditEvent>> searchEvents(@RequestBody AuditSearchRequest request) {
        return ResponseEntity.ok(auditService.searchEvents(request));
    }

    // Aggregate compliance dashboard — officers only
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<DashboardSummary> getDashboard() {
        return ResponseEntity.ok(auditService.getDashboardSummary());
    }

    @GetMapping("/high-risk")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST','COMPLIANCE_OFFICER')")
    public ResponseEntity<List<AuditEvent>> getHighRiskEvents() {
        return ResponseEntity.ok(auditService.getHighRiskEvents());
    }
}
