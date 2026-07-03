package com.audittrail.repository;

import com.audittrail.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

// JpaSpecificationExecutor adds findAll(Specification, Pageable) for dynamic search
public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    // ─── Derived queries: Spring generates the SQL from the method name ───
    List<AuditEvent> findByPerformedBy(String performedBy);
    List<AuditEvent> findByActionType(String actionType);
    List<AuditEvent> findByMerchantId(String merchantId);
    List<AuditEvent> findByRiskLevel(String riskLevel);

    // Used later to reject flagging the same transaction twice
    boolean existsByTransactionIdAndActionType(String transactionId, String actionType);

    // ─── Custom queries (JPQL) for anything the method-name syntax can't express ───

    @Query("SELECT e FROM AuditEvent e WHERE e.performedAt >= :fromDate AND e.performedAt <= :toDate ORDER BY e.performedAt DESC")
    List<AuditEvent> findByDateRange(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query("SELECT e FROM AuditEvent e WHERE e.actionType = 'FRAUD_FLAG' AND e.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY e.performedAt DESC")
    List<AuditEvent> findHighRiskFraudEvents();

    @Query("SELECT e.actionType, COUNT(e) FROM AuditEvent e GROUP BY e.actionType")
    List<Object[]> countByActionType();

    @Query("SELECT e FROM AuditEvent e ORDER BY e.performedAt DESC")
    Page<AuditEvent> findAllPaginated(Pageable pageable);

    // Flexible search is implemented with a Specification in AuditService
    // (see JpaSpecificationExecutor.findAll(Specification, Pageable)) — this
    // avoids the ":param IS NULL OR ..." pattern that Postgres can't type-infer.
}
