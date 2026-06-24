package com.medsimple.diagnostic.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.diagnostic.dao.*;
import com.medsimple.diagnostic.dto.*;
import com.medsimple.diagnostic.entity.*;
import com.medsimple.diagnostic.exception.InvalidRequisitionStateException;
import com.medsimple.diagnostic.exception.ResultNotReleasedException;
import com.medsimple.diagnostic.repository.DiagnosticRequisitionRepository;
import com.medsimple.diagnostic.repository.DiagnosticResultRepository;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
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
class DiagnosticServiceTest {

    @Mock DiagnosticRequisitionRepository requisitionRepository;
    @Mock DiagnosticResultRepository resultRepository;
    @Mock CarePlanRepository carePlanRepository;
    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock RequisitionDeliveryResolver deliveryResolver;
    @Mock RequisitionDeliveryDao deliveryDao;
    @Mock ResultSummaryDao resultSummaryDao;
    @Mock AuditService auditService;
    @Mock com.medsimple.notification.service.NotificationService notificationService;
    @Mock AccessGuard accessGuard;

    @InjectMocks DiagnosticService diagnosticService;

    private User patient;
    private User physician;
    private Clinic clinic;
    private CarePlan carePlan;
    private SymptomEpisode episode;
    private DiagnosticRequisition requisition;
    private UUID carePlanId;
    private UUID requisitionId;

    @BeforeEach
    void setUp() {
        carePlanId = UUID.randomUUID();
        requisitionId = UUID.randomUUID();
        patient = User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@x.com").build();
        physician = User.builder().id(UUID.randomUUID()).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("S").email("doc@x.com").build();
        clinic = Clinic.builder().id(UUID.randomUUID()).name("Clinic").build();
        carePlan = CarePlan.builder().id(carePlanId).patient(patient).physician(physician)
                .clinic(clinic).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
        episode = SymptomEpisode.builder().id(UUID.randomUUID()).patient(patient)
                .assignedClinic(clinic).assignedPhysician(physician).status(EpisodeStatus.IN_PROGRESS)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
        requisition = DiagnosticRequisition.builder()
                .id(requisitionId).episode(episode).carePlan(carePlan).patient(patient).orderingPhysician(physician)
                .testType("CBC").status(RequisitionStatus.DRAFT).urgency(RequisitionUrgency.ROUTINE)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    private DiagnosticResult result(OffsetDateTime releasedAt, String interpretation) {
        return DiagnosticResult.builder()
                .id(UUID.randomUUID()).requisition(requisition).patient(patient)
                .orderingPhysician(physician).rawResultText("Hb 9.1 g/dL (low)")
                .aiSummary("Your blood count is slightly low.")
                .physicianInterpretation(interpretation)
                .releasedToPatientAt(releasedAt)
                .receivedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
    }

    // ── requisition lifecycle ───────────────────────────────────────────────────

    @Test
    void createRequisition_fromEpisode_derivesPatientAndPhysician_noCarePlanNeeded() {
        UUID eid = episode.getId();
        when(episodeRepository.findById(eid)).thenReturn(Optional.of(episode));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequisitionResponse resp = diagnosticService.createRequisition(
                new CreateRequisitionRequest(eid, "CBC", null, "anemia workup", RequisitionUrgency.ROUTINE),
                "127.0.0.1");

        assertThat(resp.episodeId()).isEqualTo(eid);
        assertThat(resp.patientId()).isEqualTo(patient.getId());
        assertThat(resp.orderingPhysicianId()).isEqualTo(physician.getId());
        assertThat(resp.carePlanId()).isNull(); // ordered without a care plan
    }

    @Test
    void createRequisition_episodeNotAssignedToPhysician_throws() {
        episode.setAssignedPhysician(null);
        when(episodeRepository.findById(episode.getId())).thenReturn(Optional.of(episode));

        assertThatThrownBy(() -> diagnosticService.createRequisition(
                new CreateRequisitionRequest(episode.getId(), "CBC", null, null, RequisitionUrgency.ROUTINE), null))
                .isInstanceOf(IllegalStateException.class);
        verify(requisitionRepository, never()).save(any());
    }

    @Test
    void send_draftDispatchesAndBecomesSent() {
        when(requisitionRepository.findById(requisitionId)).thenReturn(Optional.of(requisition));
        when(deliveryResolver.resolve(clinic.getId())).thenReturn(deliveryDao);
        when(deliveryDao.deliver(requisition)).thenReturn(new RequisitionDeliveryResult(
                RequisitionStatus.SENT, DiagnosticDeliveryMethod.PDF_PRINT, "storage://req.pdf"));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequisitionResponse response = diagnosticService.sendRequisition(requisitionId, "127.0.0.1");

        assertThat(response.status()).isEqualTo(RequisitionStatus.SENT);
        assertThat(response.deliveryMethod()).isEqualTo(DiagnosticDeliveryMethod.PDF_PRINT);
        assertThat(response.documentUrl()).isEqualTo("storage://req.pdf");
    }

    @Test
    void recordResult_runsSummaryAndResultsTheRequisition() {
        when(requisitionRepository.findById(requisitionId)).thenReturn(Optional.of(requisition));
        when(resultSummaryDao.summarize(eq("CBC"), anyString())).thenReturn("Plain language summary.");
        when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhysicianResultResponse response = diagnosticService.recordResult(
                requisitionId, new RecordResultRequest("Hb 9.1 g/dL (low)", null), "127.0.0.1");

        assertThat(response.aiSummary()).isEqualTo("Plain language summary.");
        assertThat(response.releasedToPatient()).isFalse(); // gate closed by default
        assertThat(requisition.getStatus()).isEqualTo(RequisitionStatus.RESULTED);
        verify(resultSummaryDao).summarize(eq("CBC"), eq("Hb 9.1 g/dL (low)"));
    }

    // ── THE PHIPA GATE ──────────────────────────────────────────────────────────

    @Test
    void release_requiresInterpretation() {
        DiagnosticResult unreviewed = result(null, null); // no interpretation
        when(resultRepository.findById(unreviewed.getId())).thenReturn(Optional.of(unreviewed));

        assertThatThrownBy(() -> diagnosticService.releaseResult(unreviewed.getId(), null))
                .isInstanceOf(InvalidRequisitionStateException.class)
                .hasMessageContaining("interpretation is required");
    }

    @Test
    void release_withInterpretation_setsReleaseTimestamp() {
        DiagnosticResult reviewed = result(null, "Mild anemia, repeat in 4 weeks.");
        when(resultRepository.findById(reviewed.getId())).thenReturn(Optional.of(reviewed));
        when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhysicianResultResponse response = diagnosticService.releaseResult(reviewed.getId(), "127.0.0.1");

        assertThat(response.releasedToPatient()).isTrue();
        assertThat(reviewed.getReleasedToPatientAt()).isNotNull();
        verify(auditService).log(physician.getId(), "RESULT_RELEASED", "diagnostic_results",
                reviewed.getId(), "127.0.0.1");
    }

    @Test
    void patientRead_unreleasedResult_throwsNotReleased() {
        DiagnosticResult unreleased = result(null, "Mild anemia.");
        when(resultRepository.findById(unreleased.getId())).thenReturn(Optional.of(unreleased));

        assertThatThrownBy(() -> diagnosticService.getResultForPatient(unreleased.getId()))
                .isInstanceOf(ResultNotReleasedException.class);
    }

    @Test
    void patientRead_releasedResult_returnsSafeFieldsOnly() {
        DiagnosticResult released = result(OffsetDateTime.now(), "Mild anemia, repeat in 4 weeks.");
        when(resultRepository.findById(released.getId())).thenReturn(Optional.of(released));

        PatientResultResponse response = diagnosticService.getResultForPatient(released.getId());

        assertThat(response.testType()).isEqualTo("CBC");
        assertThat(response.aiSummary()).isEqualTo("Your blood count is slightly low.");
        assertThat(response.physicianInterpretation()).isEqualTo("Mild anemia, repeat in 4 weeks.");
        // PatientResultResponse has no field for raw result data — raw values never exposed.
        assertThat(response.toString()).doesNotContain("Hb 9.1");
    }

    @Test
    void patientList_returnsOnlyReleasedResults() {
        DiagnosticResult released = result(OffsetDateTime.now(), "interp");
        when(resultRepository.findByPatientIdAndReleasedToPatientAtIsNotNullOrderByReceivedAtDesc(patient.getId()))
                .thenReturn(List.of(released));

        List<PatientResultResponse> results = diagnosticService.getPatientReleasedResults(patient.getId());

        assertThat(results).hasSize(1);
        // The query itself enforces the gate — verify we called the gated finder, not the full one.
        verify(resultRepository).findByPatientIdAndReleasedToPatientAtIsNotNullOrderByReceivedAtDesc(patient.getId());
        verify(resultRepository, never()).findByPatientIdOrderByReceivedAtDesc(any());
        verify(accessGuard).requirePatientSelfOrStaff(patient.getId());
    }

    @Test
    void patientList_deniedForAnotherPatient() {
        UUID pid = patient.getId();
        doThrow(new AccessDeniedException("not your results"))
                .when(accessGuard).requirePatientSelfOrStaff(pid);

        assertThatThrownBy(() -> diagnosticService.getPatientReleasedResults(pid))
                .isInstanceOf(AccessDeniedException.class);
        verify(resultRepository, never())
                .findByPatientIdAndReleasedToPatientAtIsNotNullOrderByReceivedAtDesc(any());
    }

    @Test
    void physicianRead_unreleasedResult_stillVisible() {
        DiagnosticResult unreleased = result(null, null);
        when(resultRepository.findById(unreleased.getId())).thenReturn(Optional.of(unreleased));

        PhysicianResultResponse response = diagnosticService.getResultForPhysician(unreleased.getId());

        assertThat(response.releasedToPatient()).isFalse();
        assertThat(response.rawResultText()).isEqualTo("Hb 9.1 g/dL (low)");
    }
}
