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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PatientHealthCoverageRepository coverageRepository;
    private final PatientClinicRegistrationRepository registrationRepository;
    private final PatientConsentRepository consentRepository;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EntityManager entityManager;

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String ipAddress) {
        if (patientRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.PATIENT)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
        User saved = patientRepository.save(user);

        PatientProfile profile = PatientProfile.builder()
                .user(saved)
                .dateOfBirth(request.dateOfBirth())
                .build();
        PatientProfile savedProfile = patientProfileRepository.save(profile);

        auditService.log(saved.getId(), "PATIENT_CREATED", "users", saved.getId(), ipAddress);

        return toResponse(saved, savedProfile);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatient(UUID id) {
        User user = findPatientById(id);
        PatientProfile profile = patientProfileRepository.findByUserId(id).orElseThrow();
        return toResponse(user, profile);
    }

    @Transactional
    public PatientResponse updateProfile(UUID patientId, UpdatePatientProfileRequest request) {
        User user = findPatientById(patientId);
        PatientProfile profile = patientProfileRepository.findByUserId(patientId).orElseThrow();

        if (request.dateOfBirth() != null) profile.setDateOfBirth(request.dateOfBirth());
        if (request.preferredPharmacyId() != null) profile.setPreferredPharmacyId(request.preferredPharmacyId());
        if (request.allergies() != null) profile.setAllergies(request.allergies());
        if (request.currentMedications() != null) profile.setCurrentMedications(request.currentMedications());

        PatientProfile updated = patientProfileRepository.save(profile);
        auditService.log(patientId, "PATIENT_PROFILE_UPDATED", "patient_profiles", updated.getId(), null);

        return toResponse(user, updated);
    }

    @Transactional
    public HealthCoverageResponse addHealthCoverage(UUID patientId, AddHealthCoverageRequest request,
                                                     String ipAddress) {
        User patient = findPatientById(patientId);

        PatientHealthCoverage coverage = PatientHealthCoverage.builder()
                .patient(patient)
                .coverageType(request.coverageType())
                .providerName(request.providerName())
                .cardNumberEnc(encryptionService.encrypt(request.cardNumber()))
                .province(request.province())
                .expiryDate(request.expiryDate())
                .primary(request.primary())
                .build();

        PatientHealthCoverage saved = coverageRepository.save(coverage);
        auditService.log(patientId, "COVERAGE_ADDED", "patient_health_coverage", saved.getId(), ipAddress);

        return toCoverageResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HealthCoverageResponse> getHealthCoverage(UUID patientId) {
        findPatientById(patientId);
        return coverageRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(this::toCoverageResponse)
                .toList();
    }

    @Transactional
    public void deactivateHealthCoverage(UUID patientId, UUID coverageId) {
        PatientHealthCoverage coverage = coverageRepository
                .findByIdAndPatientId(coverageId, patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        coverage.setActive(false);
        coverageRepository.save(coverage);
        auditService.log(patientId, "COVERAGE_DEACTIVATED", "patient_health_coverage", coverageId, null);
    }

    @Transactional
    public ClinicRegistrationResponse registerToClinic(UUID patientId, RegisterToClinicRequest request,
                                                        String ipAddress) {
        User patient = findPatientById(patientId);

        if (registrationRepository.existsByPatientIdAndClinicId(patientId, request.clinicId())) {
            throw new PatientAlreadyRegisteredException(patientId, request.clinicId());
        }

        // Use EntityManager reference to avoid loading the full Clinic entity.
        Clinic clinicRef = entityManager.getReference(Clinic.class, request.clinicId());

        PatientClinicRegistration registration = PatientClinicRegistration.builder()
                .patient(patient)
                .clinic(clinicRef)
                .build();

        PatientClinicRegistration saved = registrationRepository.save(registration);
        auditService.log(patientId, "CLINIC_REGISTERED", "patient_clinic_registrations",
                saved.getId(), ipAddress);

        return toRegistrationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicRegistrationResponse> getClinicRegistrations(UUID patientId) {
        findPatientById(patientId);
        return registrationRepository.findByPatientId(patientId)
                .stream()
                .map(this::toRegistrationResponse)
                .toList();
    }

    @Transactional
    public ConsentResponse recordConsent(UUID patientId, RecordConsentRequest request, String ipAddress) {
        User patient = findPatientById(patientId);

        PatientConsent consent = PatientConsent.builder()
                .patient(patient)
                .consentType(request.consentType())
                .version(request.version())
                .grantedAt(request.grantedAt())
                .revokedAt(request.revokedAt())
                .ipAddress(ipAddress)
                .build();

        PatientConsent saved = consentRepository.save(consent);
        auditService.log(patientId, "CONSENT_RECORDED", "patient_consents", saved.getId(), ipAddress);

        return toConsentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getConsents(UUID patientId) {
        findPatientById(patientId);
        return consentRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::toConsentResponse)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findPatientById(UUID id) {
        return patientRepository.findByIdAndRole(id, UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    private PatientResponse toResponse(User user, PatientProfile profile) {
        return new PatientResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.isActive(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                new PatientResponse.Profile(
                        profile.getDateOfBirth(),
                        profile.getPreferredPharmacyId(),
                        profile.getAllergies(),
                        profile.getCurrentMedications()
                )
        );
    }

    private HealthCoverageResponse toCoverageResponse(PatientHealthCoverage c) {
        return new HealthCoverageResponse(
                c.getId(),
                c.getCoverageType(),
                c.getProviderName(),
                c.getProvince(),
                c.getExpiryDate(),
                c.isPrimary(),
                c.isActive(),
                c.getCreatedAt()
        );
    }

    private ClinicRegistrationResponse toRegistrationResponse(PatientClinicRegistration r) {
        return new ClinicRegistrationResponse(
                r.getId(),
                r.getClinic().getId(),
                r.getClinic().getName(),
                r.getAssignedPhysician() != null ? r.getAssignedPhysician().getId() : null,
                r.isActive(),
                r.getRegisteredAt()
        );
    }

    private ConsentResponse toConsentResponse(PatientConsent c) {
        return new ConsentResponse(
                c.getId(),
                c.getConsentType(),
                c.getVersion(),
                c.getGrantedAt(),
                c.getRevokedAt(),
                c.getCreatedAt()
        );
    }
}
