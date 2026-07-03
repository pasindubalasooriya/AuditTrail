package com.audittrail.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // TEMPORARY (Phase 5): identity comes from a header so we can test.
    // Phase 6 replaces this with the verified username from the JWT.
    private String currentUser(String headerUser) {
        return (headerUser != null && !headerUser.isBlank()) ? headerUser : "dev-user";
    }

    @PostMapping("/refund")
    public ResponseEntity<AuditEvent> recordRefund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(value = "X-User", required = false) String user,
            HttpServletRequest httpRequest) {
        AuditEvent saved = auditService.recordRefund(request, currentUser(user), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/fraud-flag")
    public ResponseEntity<AuditEvent> recordFraudFlag(
            @Valid @RequestBody FraudFlagRequest request,
            @RequestHeader(value = "X-User", required = false) String user,
            HttpServletRequest httpRequest) {
        AuditEvent saved = auditService.recordFraudFlag(request, currentUser(user), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/events")
    public ResponseEntity<List<AuditEvent>> getAllEvents() {
        return ResponseEntity.ok(auditService.getAllEvents());
    }

    @GetMapping("/events/by-type")
    public ResponseEntity<List<AuditEvent>> getEventsByType(@RequestParam String type) {
        return ResponseEntity.ok(auditService.getEventsByType(type));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<AuditEvent>> searchEvents(@RequestBody AuditSearchRequest request) {
        return ResponseEntity.ok(auditService.searchEvents(request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummary> getDashboard() {
        return ResponseEntity.ok(auditService.getDashboardSummary());
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<AuditEvent>> getHighRiskEvents() {
        return ResponseEntity.ok(auditService.getHighRiskEvents());
    }
}