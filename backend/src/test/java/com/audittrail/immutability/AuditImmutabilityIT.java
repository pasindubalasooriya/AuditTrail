package com.audittrail.immutability;

import com.audittrail.model.AuditEvent;
import com.audittrail.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the headline feature against a REAL PostgreSQL (H2 would not run the plpgsql trigger).
 * Testcontainers boots postgres:16, Flyway applies V1 + V2, then we assert that a direct
 * UPDATE and DELETE are both rejected by the immutability trigger.
 */
@SpringBootTest
@Testcontainers
class AuditImmutabilityIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        // Point both the runtime datasource and Flyway at the throwaway container
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private AuditEventRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void immutabilityTrigger_rejectsUpdateAndDelete() {
        AuditEvent saved = repository.save(new AuditEvent("REFUND", "tester"));
        Long id = saved.getId();
        assertThat(id).isNotNull();

        // The DB trigger must reject a direct UPDATE...
        assertThatThrownBy(() ->
                jdbc.update("UPDATE audit_events SET performed_by = 'hacker' WHERE id = ?", id))
                .hasStackTraceContaining("immutable");

        // ...and a direct DELETE
        assertThatThrownBy(() ->
                jdbc.update("DELETE FROM audit_events WHERE id = ?", id))
                .hasStackTraceContaining("immutable");

        // The record is still intact
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE id = ?", Integer.class, id);
        assertThat(count).isEqualTo(1);
    }
}
