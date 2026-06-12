package com.medsimple.patient.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.patient.dto.*;
import com.medsimple.patient.entity.*;
import com.medsimple.patient.exception.EmailAlreadyInUseException;
import com.medsimple.patient.exception.PatientAlreadyRegisteredException;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.*;
import com.medsimple.shared.crypto.EncryptionService;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock PatientProfileRepository patientProfileRepository;
    @Mock PatientHealthCoverageRepository coverageRepository;
    @Mock PatientClinicRegistrationRepository registrationRepository;
    @Mock PatientConsentRepository consentRepository;
    @Mock EncryptionService encryptionService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;
    @Mock EntityManager entityManager;

    @InjectMocks PatientService patientService;

    private User sampleUser;
    private PatientProfile sampleProfile;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("jane@example.com")
                .passwordHash("hashed")
                .role(UserRole.PATIENT)
                .firstName("Jane")
                .lastName("Doe")
                .createdAt(OffsetDateTime.now())
                .build();
        sampleProfile = PatientProfile.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .build();
    }

    // ── createPatient ────────────────────────────────────────────────────────

    @Test
    void createPatient_success() {
        CreatePatientRequest request = new CreatePatientRequest(
                "jane@example.com", "password123", "Jane", "Doe", null, null);

        when(patientRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(patientRepository.save(any())).thenReturn(sampleUser);
        when(patientProfileRepository.save(any())).thenReturn(sampleProfile);

        PatientResponse response = patientService.createPatient(request, "127.0.0.1");

        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.firstName()).isEqualTo("Jane");
        verify(auditService).log(userId, "PATIENT_CREATED", "users", userId, "127.0.0.1");
    }

    @Test
    void createPatient_duplicateEmail_throws() {
        when(patientRepository.existsByEmail("jane@example.com")).thenReturn(true);
        CreatePatientRequest request = new CreatePatientRequest(
                "jane@example.com", "password123", "Jane", "Doe", null, null);

        assertThatThrownBy(() -> patientService.createPatient(request, null))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessageContaining("jane@example.com");
    }

    // ── getPatient ───────────────────────────────────────────────────────────

    @Test
    void getPatient_found() {
        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.of(sampleUser));
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.of(sampleProfile));

        PatientResponse response = patientService.getPatient(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("jane@example.com");
    }

    @Test
    void getPatient_notFound_throws() {
        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatient(userId))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    // ── addHealthCoverage ────────────────────────────────────────────────────

    @Test
    void addHealthCoverage_encryptsCardNumber() {
        AddHealthCoverageRequest request = new AddHealthCoverageRequest(
                CoverageType.PROVINCIAL, "Ontario OHIP", "AB123456", "ON", null, true);

        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.of(sampleUser));
        when(encryptionService.encrypt("AB123456")).thenReturn("enc_value");

        PatientHealthCoverage savedCoverage = PatientHealthCoverage.builder()
                .id(UUID.randomUUID())
                .patient(sampleUser)
                .coverageType(CoverageType.PROVINCIAL)
                .providerName("Ontario OHIP")
                .cardNumberEnc("enc_value")
                .province("ON")
                .primary(true)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
        when(coverageRepository.save(any())).thenReturn(savedCoverage);

        HealthCoverageResponse response = patientService.addHealthCoverage(userId, request, "127.0.0.1");

        verify(encryptionService).encrypt("AB123456");
        assertThat(response.coverageType()).isEqualTo(CoverageType.PROVINCIAL);
        assertThat(response.providerName()).isEqualTo("Ontario OHIP");
    }

    @Test
    void addHealthCoverage_rawCardNumberNotInResponse() {
        AddHealthCoverageRequest request = new AddHealthCoverageRequest(
                CoverageType.PROVINCIAL, "Ontario OHIP", "SENSITIVE_CARD_NUM", "ON", null, false);

        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.of(sampleUser));
        when(encryptionService.encrypt(any())).thenReturn("encrypted_blob");

        PatientHealthCoverage savedCoverage = PatientHealthCoverage.builder()
                .id(UUID.randomUUID())
                .patient(sampleUser)
                .coverageType(CoverageType.PROVINCIAL)
                .providerName("Ontario OHIP")
                .cardNumberEnc("encrypted_blob")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
        when(coverageRepository.save(any())).thenReturn(savedCoverage);

        HealthCoverageResponse response = patientService.addHealthCoverage(userId, request, null);

        // HealthCoverageResponse has no field for the card number
        assertThat(response).isInstanceOf(HealthCoverageResponse.class);
        assertThat(response.toString()).doesNotContain("SENSITIVE_CARD_NUM");
        assertThat(response.toString()).doesNotContain("encrypted_blob");
    }

    // ── registerToClinic ─────────────────────────────────────────────────────

    @Test
    void registerToClinic_success() {
        UUID clinicId = UUID.randomUUID();
        RegisterToClinicRequest request = new RegisterToClinicRequest(clinicId);
        Clinic clinicRef = Clinic.builder().id(clinicId).name("Maple Clinic").build();

        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.of(sampleUser));
        when(registrationRepository.existsByPatientIdAndClinicId(userId, clinicId)).thenReturn(false);
        when(entityManager.getReference(Clinic.class, clinicId)).thenReturn(clinicRef);

        PatientClinicRegistration savedReg = PatientClinicRegistration.builder()
                .id(UUID.randomUUID())
                .patient(sampleUser)
                .clinic(clinicRef)
                .active(true)
                .registeredAt(OffsetDateTime.now())
                .build();
        when(registrationRepository.save(any())).thenReturn(savedReg);

        ClinicRegistrationResponse response = patientService.registerToClinic(userId, request, "127.0.0.1");

        assertThat(response.clinicId()).isEqualTo(clinicId);
        assertThat(response.clinicName()).isEqualTo("Maple Clinic");
    }

    @Test
    void registerToClinic_alreadyRegistered_throws() {
        UUID clinicId = UUID.randomUUID();
        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.of(sampleUser));
        when(registrationRepository.existsByPatientIdAndClinicId(userId, clinicId)).thenReturn(true);

        assertThatThrownBy(() -> patientService.registerToClinic(
                userId, new RegisterToClinicRequest(clinicId), null))
                .isInstanceOf(PatientAlreadyRegisteredException.class);
    }

    // ── recordConsent ────────────────────────────────────────────────────────

    @Test
    void recordConsent_appendsNewRow() {
        RecordConsentRequest request = new RecordConsentRequest(
                ConsentType.DATA_PROCESSING, "v1.0", OffsetDateTime.now(), null);

        when(patientRepository.findByIdAndRole(userId, UserRole.PATIENT)).thenReturn(Optional.of(sampleUser));

        PatientConsent saved = PatientConsent.builder()
                .id(UUID.randomUUID())
                .patient(sampleUser)
                .consentType(ConsentType.DATA_PROCESSING)
                .version("v1.0")
                .grantedAt(request.grantedAt())
                .createdAt(OffsetDateTime.now())
                .build();
        when(consentRepository.save(any())).thenReturn(saved);

        ConsentResponse response = patientService.recordConsent(userId, request, "127.0.0.1");

        assertThat(response.consentType()).isEqualTo(ConsentType.DATA_PROCESSING);
        assertThat(response.version()).isEqualTo("v1.0");
        assertThat(response.revokedAt()).isNull();
    }
}
