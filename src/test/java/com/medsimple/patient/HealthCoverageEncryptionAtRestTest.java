package com.medsimple.patient;

import com.medsimple.patient.entity.CoverageType;
import com.medsimple.patient.entity.PatientHealthCoverage;
import com.medsimple.patient.repository.PatientHealthCoverageRepository;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.crypto.EncryptionService;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the PHIPA at-rest guarantee for health-card / insurance numbers (CLAUDE.md: "Health card
 * and insurance numbers: AES-256 encrypted at application layer before any DB write. Never log or
 * return raw values.").
 *
 * <p>The write path mirrors {@code PatientService.addHealthCoverage}: the card number is run
 * through {@link EncryptionService#encrypt} before it is placed on the entity, and only the
 * ciphertext is ever handed to JPA. This test then bypasses JPA and reads the raw {@code
 * card_number_enc} column straight off the database with {@link JdbcTemplate}, asserting the bytes
 * on disk are NOT the plaintext and that they decrypt back to the original. Synthetic data only.
 */
class HealthCoverageEncryptionAtRestTest extends PostgresIntegrationTest {

    // Synthetic OHIP-style value — not a real health card.
    private static final String PLAINTEXT_CARD = "1234567890AB";

    @Autowired PatientRepository patientRepository;
    @Autowired PatientHealthCoverageRepository coverageRepository;
    @Autowired EncryptionService encryptionService;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void cardNumber_storedAsCiphertext_andDecryptsBack() {
        User patient = patientRepository.save(User.builder()
                .email("enc-at-rest-" + UUID.randomUUID() + "@example.com")
                .passwordHash("x")
                .role(UserRole.PATIENT)
                .firstName("Test")
                .lastName("Patient")
                .build());

        // Same write path as PatientService.addHealthCoverage: encrypt BEFORE the entity is built.
        PatientHealthCoverage saved = coverageRepository.save(PatientHealthCoverage.builder()
                .patient(patient)
                .coverageType(CoverageType.PROVINCIAL)
                .providerName("OHIP")
                .cardNumberEnc(encryptionService.encrypt(PLAINTEXT_CARD))
                .province("ON")
                .primary(true)
                .build());

        // Read the RAW column value, bypassing JPA entirely. This is what actually sits on disk.
        String rawOnDisk = jdbcTemplate.queryForObject(
                "SELECT card_number_enc FROM patient_health_coverage WHERE id = ?",
                String.class, saved.getId());

        assertThat(rawOnDisk)
                .as("stored column must never equal the plaintext card number")
                .isNotNull()
                .isNotEqualTo(PLAINTEXT_CARD)
                .doesNotContain(PLAINTEXT_CARD);

        // The plaintext must not appear even as a substring (catches accidental partial storage).
        assertThat(rawOnDisk).doesNotContain("1234567890");

        // And the ciphertext round-trips back to the exact original via the same key.
        assertThat(encryptionService.decrypt(rawOnDisk))
                .as("ciphertext on disk must decrypt back to the original card number")
                .isEqualTo(PLAINTEXT_CARD);
    }
}
