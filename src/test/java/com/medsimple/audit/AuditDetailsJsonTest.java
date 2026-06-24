package com.medsimple.audit;

import com.medsimple.audit.service.AuditService;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the structured-details overload of {@link AuditService#log} writes VALID JSON to the
 * {@code audit_logs.details} jsonb column.
 *
 * <p>Regression guard for a real bug: a caller (media upload) passed a plain string
 * ("image/png;122b") to a jsonb column and every request 500'd with "invalid input syntax for type
 * json". Unit tests mocked AuditService, so the database contract was never exercised. The overload
 * now takes a Map and serializes it, and this test reads the raw column back through Postgres'
 * jsonb operators — which only work if the stored value is real JSON.
 */
class AuditDetailsJsonTest extends PostgresIntegrationTest {

    @Autowired AuditService auditService;
    @Autowired PatientRepository patientRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void log_withStructuredDetails_storesValidJsonb() {
        User actor = patientRepository.save(User.builder()
                .email("audit-json-" + UUID.randomUUID() + "@example.com")
                .passwordHash("x").role(UserRole.PATIENT)
                .firstName("Json").lastName("Actor").build());

        UUID resourceId = UUID.randomUUID();
        auditService.log(actor.getId(), "MEDIA_UPLOADED", "symptom_media", resourceId,
                "203.0.113.7", Map.of("contentType", "image/png", "bytes", 122));

        // Read individual fields back via Postgres jsonb operators. These throw if the column does
        // not hold valid JSON, so a successful, correct read is the proof.
        String contentType = jdbcTemplate.queryForObject(
                "SELECT details->>'contentType' FROM audit_logs "
                        + "WHERE resource_id = ? AND action = 'MEDIA_UPLOADED'",
                String.class, resourceId);
        Integer bytes = jdbcTemplate.queryForObject(
                "SELECT (details->>'bytes')::int FROM audit_logs "
                        + "WHERE resource_id = ? AND action = 'MEDIA_UPLOADED'",
                Integer.class, resourceId);

        assertThat(contentType).isEqualTo("image/png");
        assertThat(bytes).isEqualTo(122);
    }
}
