package com.medsimple.audit;

import com.medsimple.audit.repository.AuditLogRepository;
import com.medsimple.audit.service.AuditService;
import com.medsimple.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the PHIPA append-only guarantee for {@code audit_logs} (CLAUDE.md: "audit_logs:
 * append-only. No UPDATE or DELETE ever.") is enforced at the DATABASE layer by the V2
 * immutability triggers, not merely by application convention.
 *
 * <p>1. {@link AuditService#log} inserts a new row (the legitimate path still works).
 * <p>2. A raw {@code UPDATE} on that row is rejected by {@code trg_audit_logs_no_update}.
 * <p>3. A raw {@code DELETE} on that row is rejected by {@code trg_audit_logs_no_delete}.
 *
 * <p>The UPDATE/DELETE go through {@link JdbcTemplate} (raw SQL, bypassing JPA) precisely to model
 * a direct database connection or an application bug — the trigger must stop both regardless of how
 * the statement arrives.
 */
class AuditLogImmutabilityTest extends PostgresIntegrationTest {

    @Autowired AuditService auditService;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void log_insertsRow_butUpdateAndDeleteAreRejectedByTrigger() {
        long before = countAuditRows();

        UUID actorId = UUID.randomUUID();
        auditService.log(actorId, "TEST_ACTION", "audit_logs_immutability_test",
                UUID.randomUUID(), "203.0.113.7");

        // 1. The append worked: exactly one new row.
        assertThat(countAuditRows()).isEqualTo(before + 1);

        UUID rowId = jdbcTemplate.queryForObject(
                "SELECT id FROM audit_logs WHERE action = 'TEST_ACTION' "
                        + "ORDER BY created_at DESC LIMIT 1",
                UUID.class);
        assertThat(rowId).isNotNull();

        // 2. UPDATE is blocked by the DB trigger.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE audit_logs SET action = 'TAMPERED' WHERE id = ?", rowId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        // 3. DELETE is blocked by the DB trigger.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE id = ?", rowId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        // The row is still present and untouched after both rejected attempts.
        String action = jdbcTemplate.queryForObject(
                "SELECT action FROM audit_logs WHERE id = ?", String.class, rowId);
        assertThat(action).isEqualTo("TEST_ACTION");
    }

    private long countAuditRows() {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);
        return n == null ? 0L : n;
    }
}
