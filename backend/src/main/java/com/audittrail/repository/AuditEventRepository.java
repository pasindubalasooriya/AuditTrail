package com.audittrail.repository;

import com.audittrail.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

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

    // ─── Flexible search: every filter is optional (null = "ignore this filter") ───
    // No ORDER BY here — sorting is supplied by the Pageable the service passes in.
    @Query("SELECT e FROM AuditEvent e WHERE " +
           "(:actionType IS NULL OR e.actionType = :actionType) AND " +
           "(:merchantId IS NULL OR e.merchantId = :merchantId) AND " +
           "(:performedBy IS NULL OR e.performedBy = :performedBy) AND " +
           "(:riskLevel IS NULL OR e.riskLevel = :riskLevel) AND " +
           "(:fromDate IS NULL OR e.performedAt >= :fromDate) AND " +
           "(:toDate IS NULL OR e.performedAt <= :toDate)")
    Page<AuditEvent> searchEvents(
        @Param("actionType") String actionType,
        @Param("merchantId") String merchantId,
        @Param("performedBy") String performedBy,
        @Param("riskLevel") String riskLevel,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        Pageable pageable
    );
}
