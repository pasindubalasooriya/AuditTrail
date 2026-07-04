package com.audittrail.controller;

import com.audittrail.config.JwtUtils;
import com.audittrail.config.SecurityConfig;
import com.audittrail.config.SecurityExceptionHandler;
import com.audittrail.model.AuditEvent;
import com.audittrail.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The RBAC matrix, made executable. Uses @WithMockUser to simulate authenticated
 * users with roles (no real WSO2 token needed), and asserts the @PreAuthorize guards.
 */
@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class, JwtUtils.class})
class AuditControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    // The oauth2 resource server config needs a JwtDecoder bean; we don't exercise it here.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void noToken_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/audit/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "sara", roles = "FRAUD_ANALYST")
    void analyst_canReadSharedEvents() throws Exception {
        mockMvc.perform(get("/api/audit/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "sara", roles = "FRAUD_ANALYST")
    void analyst_cannotSeeComplianceDashboard_403() throws Exception {
        mockMvc.perform(get("/api/audit/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sara", roles = "FRAUD_ANALYST")
    void analyst_canFlagFraud_201() throws Exception {
        when(auditService.recordFraudFlag(any(), anyString(), anyString()))
                .thenReturn(new AuditEvent("FRAUD_FLAG", "sara"));
        mockMvc.perform(post("/api/audit/fraud-flag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":\"T1\",\"merchantId\":\"M1\",\"riskLevel\":\"HIGH\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "joe", roles = "COMPLIANCE_OFFICER")
    void officer_canSeeComplianceDashboard_200() throws Exception {
        mockMvc.perform(get("/api/audit/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "joe", roles = "COMPLIANCE_OFFICER")
    void officer_cannotFlagFraud_403() throws Exception {
        // Valid body so validation passes and the 403 comes from @PreAuthorize, not @Valid
        mockMvc.perform(post("/api/audit/fraud-flag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":\"T1\",\"merchantId\":\"M1\",\"riskLevel\":\"HIGH\"}"))
                .andExpect(status().isForbidden());
    }
}
