package com.medsimple.prescription.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.patient.entity.PatientProfile;
import com.medsimple.patient.repository.PatientProfileRepository;
import com.medsimple.prescription.dao.*;
import com.medsimple.prescription.dto.CreatePrescriptionRequest;
import com.medsimple.prescription.dto.PrescriptionResponse;
import com.medsimple.prescription.dto.SendPrescriptionRequest;
import com.medsimple.prescription.entity.DeliveryMethod;
import com.medsimple.prescription.entity.Prescription;
import com.medsimple.prescription.entity.PrescriptionStatus;
import com.medsimple.prescription.exception.InvalidPrescriptionStateException;
import com.medsimple.prescription.repository.PrescriptionRepository;
import com.medsimple.shared.dto.AllergyDto;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock PrescriptionRepository prescriptionRepository;
    @Mock CarePlanRepository carePlanRepository;
    @Mock PatientProfileRepository patientProfileRepository;
    @Mock MedicationSafetyDao medicationSafetyDao;
    @Mock PrescriptionDeliveryResolver deliveryResolver;
    @Mock PrescriptionDeliveryDao deliveryDao;
    @Mock AuditService auditService;
    @Mock AccessGuard accessGuard;

    @InjectMocks PrescriptionService prescriptionService;

    private User patient;
    private User physician;
    private Clinic clinic;
    private CarePlan carePlan;
    private UUID carePlanId;

    @BeforeEach
    void setUp() {
        carePlanId = UUID.randomUUID();
        patient = User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@x.com").build();
        physician = User.builder().id(UUID.randomUUID()).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("S").email("doc@x.com").build();
        clinic = Clinic.builder().id(UUID.randomUUID()).name("Clinic").build();
        carePlan = CarePlan.builder().id(carePlanId).patient(patient).physician(physician)
                .clinic(clinic).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    private Prescription draft() {
        return Prescription.builder()
                .id(UUID.randomUUID()).carePlan(carePlan).patient(patient)
                .prescribingPhysician(physician).medicationName("Amoxicillin")
                .dosage("500mg").frequency("TID").refills(0).aiSafetyChecks("{}")
                .status(PrescriptionStatus.DRAFT)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    // ── create + safety check ───────────────────────────────────────────────────

    @Test
    void create_runsSafetyCheckWithPatientAllergies() {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                carePlanId, "Amoxicillin", null, "500mg", "TID", null, 0, "Take with food");

        PatientProfile profile = PatientProfile.builder()
                .user(patient)
                .allergies(List.of(new AllergyDto("Penicillin", "HIGH")))
                .currentMedications(List.of())
                .build();

        when(carePlanRepository.findById(carePlanId)).thenReturn(Optional.of(carePlan));
        when(patientProfileRepository.findByUserId(patient.getId())).thenReturn(Optional.of(profile));
        when(medicationSafetyDao.check(any())).thenReturn(
                new SafetyCheckResult(List.of(), List.of("allergy to Penicillin"), List.of()));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = prescriptionService.createPrescription(request, "127.0.0.1");

        // The safety check received the patient's allergy list (PHI-stripped to names).
        verify(medicationSafetyDao).check(argThat(ctx ->
                ctx.allergies().contains("Penicillin") && ctx.medicationName().equals("Amoxicillin")));
        assertThat(response.status()).isEqualTo(PrescriptionStatus.DRAFT);
        assertThat(response.aiSafetyChecks()).contains("Penicillin");
    }

    // ── state machine ───────────────────────────────────────────────────────────

    @Test
    void sign_draftBecomesSigned() {
        Prescription rx = draft();
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = prescriptionService.signPrescription(rx.getId(), "127.0.0.1");

        assertThat(response.status()).isEqualTo(PrescriptionStatus.SIGNED);
        assertThat(rx.getSignedAt()).isNotNull();
    }

    @Test
    void sign_nonDraft_throws() {
        Prescription rx = draft();
        rx.setStatus(PrescriptionStatus.SIGNED);
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));

        assertThatThrownBy(() -> prescriptionService.signPrescription(rx.getId(), null))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void send_signedPrescription_pdfFallbackPath() {
        Prescription rx = draft();
        rx.setStatus(PrescriptionStatus.SIGNED);
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));
        when(deliveryResolver.resolve(clinic.getId())).thenReturn(deliveryDao);
        when(deliveryDao.deliver(rx)).thenReturn(new DeliveryResult(
                PrescriptionStatus.SENT_PDF, DeliveryMethod.PDF_PRINT, "storage://rx.pdf"));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = prescriptionService.sendPrescription(
                rx.getId(), new SendPrescriptionRequest(null), "127.0.0.1");

        assertThat(response.status()).isEqualTo(PrescriptionStatus.SENT_PDF);
        assertThat(response.deliveryMethod()).isEqualTo(DeliveryMethod.PDF_PRINT);
        assertThat(response.documentUrl()).isEqualTo("storage://rx.pdf");
        assertThat(rx.getSentAt()).isNotNull();
    }

    @Test
    void send_unsignedPrescription_throws() {
        Prescription rx = draft(); // still DRAFT
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));

        assertThatThrownBy(() -> prescriptionService.sendPrescription(rx.getId(), null, null))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void fill_sentPrescriptionBecomesFilled() {
        Prescription rx = draft();
        rx.setStatus(PrescriptionStatus.SENT_PDF);
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = prescriptionService.markFilled(rx.getId(), "127.0.0.1");

        assertThat(response.status()).isEqualTo(PrescriptionStatus.FILLED);
        assertThat(rx.getFilledAt()).isNotNull();
    }

    @Test
    void cancel_filledPrescription_throws() {
        Prescription rx = draft();
        rx.setStatus(PrescriptionStatus.FILLED);
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));

        assertThatThrownBy(() -> prescriptionService.cancelPrescription(rx.getId(), null))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void cancel_draftPrescription_succeeds() {
        Prescription rx = draft();
        when(prescriptionRepository.findById(rx.getId())).thenReturn(Optional.of(rx));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = prescriptionService.cancelPrescription(rx.getId(), "127.0.0.1");

        assertThat(response.status()).isEqualTo(PrescriptionStatus.CANCELLED);
    }

    @Test
    void getPatientPrescriptions_checksOwnershipThenReturns() {
        UUID pid = patient.getId();
        when(prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(pid)).thenReturn(List.of(draft()));

        List<PrescriptionResponse> out = prescriptionService.getPatientPrescriptions(pid);

        assertThat(out).hasSize(1);
        verify(accessGuard).requirePatientSelfOrStaff(pid);
    }

    @Test
    void getPatientPrescriptions_deniedForAnotherPatient() {
        UUID pid = patient.getId();
        doThrow(new AccessDeniedException("not your prescriptions"))
                .when(accessGuard).requirePatientSelfOrStaff(pid);

        assertThatThrownBy(() -> prescriptionService.getPatientPrescriptions(pid))
                .isInstanceOf(AccessDeniedException.class);
        verify(prescriptionRepository, never()).findByPatientIdOrderByCreatedAtDesc(any());
    }
}
