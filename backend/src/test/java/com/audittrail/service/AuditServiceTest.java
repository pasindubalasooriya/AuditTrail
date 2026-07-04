package com.audittrail.service;

import com.audittrail.dto.FraudFlagRequest;
import com.audittrail.dto.RefundRequest;
import com.audittrail.exception.DuplicateFraudFlagException;
import com.audittrail.model.AuditEvent;
import com.audittrail.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditService service;

    @Test
    void recordRefund_stampsIdentityFromArgument_neverFromRequest() {
        RefundRequest req = new RefundRequest();
        req.setTransactionId("TXN_1");
        req.setMerchantId("MER_1");
        req.setAmount(new BigDecimal("50.00"));
        when(repository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditEvent result = service.recordRefund(req, "sara", "1.2.3.4");

        // performedBy comes from the (verified) argument, not any request field
        assertThat(result.getPerformedBy()).isEqualTo("sara");
        assertThat(result.getActionType()).isEqualTo("REFUND");
        assertThat(result.getAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getIpAddress()).isEqualTo("1.2.3.4");
        verify(repository).save(any(AuditEvent.class));
    }

    @Test
    void recordFraudFlag_whenTransactionAlreadyFlagged_throwsAndDoesNotSave() {
        FraudFlagRequest req = new FraudFlagRequest();
        req.setTransactionId("TXN_DUP");
        req.setRiskLevel("HIGH");
        when(repository.existsByTransactionIdAndActionType("TXN_DUP", "FRAUD_FLAG")).thenReturn(true);

        assertThatThrownBy(() -> service.recordFraudFlag(req, "sara", "1.2.3.4"))
                .isInstanceOf(DuplicateFraudFlagException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void getDashboardSummary_aggregatesCountsAndHighRisk() {
        when(repository.count()).thenReturn(3L);
        when(repository.countByActionType()).thenReturn(List.of(
                new Object[]{"REFUND", 2L},
                new Object[]{"FRAUD_FLAG", 1L}));
        when(repository.findHighRiskFraudEvents())
                .thenReturn(List.of(new AuditEvent("FRAUD_FLAG", "sara")));

        var summary = service.getDashboardSummary();

        assertThat(summary.getTotalEvents()).isEqualTo(3);
        assertThat(summary.getTotalRefunds()).isEqualTo(2);
        assertThat(summary.getTotalFraudFlags()).isEqualTo(1);
        assertThat(summary.getHighRiskEvents()).isEqualTo(1);
    }
}
