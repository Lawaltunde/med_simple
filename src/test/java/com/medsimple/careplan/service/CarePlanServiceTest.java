package com.medsimple.careplan.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.careplan.dto.*;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.entity.CarePlanStatus;
import com.medsimple.careplan.entity.FollowUp;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.careplan.repository.FollowUpRepository;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceTest {

    @Mock CarePlanRepository carePlanRepository;
    @Mock FollowUpRepository followUpRepository;
    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock PatientRepository userRepository;
    @Mock AuditService auditService;
    @Mock EntityManager entityManager;

    @InjectMocks CarePlanService carePlanService;

    private User patient;
    private User physician;
    private Clinic clinic;
    private SymptomEpisode episode;
    private UUID episodeId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        episodeId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        patient = User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@x.com").build();
        physician = User.builder().id(physicianId).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("Smith").email("doc@x.com").build();
        clinic = Clinic.builder().id(UUID.randomUUID()).name("Clinic").build();
        episode = SymptomEpisode.builder()
                .id(episodeId).patient(patient).assignedClinic(clinic)
                .status(EpisodeStatus.ASSIGNED)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    private CarePlan sampleCarePlan() {
        return CarePlan.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .physician(physician).clinic(clinic).status(CarePlanStatus.ACTIVE)
                .notes("Rest and fluids").createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    // ── createCarePlan ──────────────────────────────────────────────────────────

    @Test
    void createCarePlan_success_movesEpisodeToInProgress() {
        CreateCarePlanRequest request = new CreateCarePlanRequest(episodeId, physicianId, "Rest and fluids");
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)).thenReturn(Optional.of(physician));
        when(carePlanRepository.save(any())).thenReturn(sampleCarePlan());

        CarePlanResponse response = carePlanService.createCarePlan(request, "127.0.0.1");

        assertThat(response.status()).isEqualTo(CarePlanStatus.ACTIVE);
        assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.IN_PROGRESS);
        verify(episodeRepository).save(episode);
        verify(auditService).log(eq(physicianId), eq("CARE_PLAN_CREATED"), eq("care_plans"), any(), eq("127.0.0.1"));
    }

    @Test
    void createCarePlan_episodeNotFound_throws() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> carePlanService.createCarePlan(
                new CreateCarePlanRequest(episodeId, physicianId, null), null))
                .isInstanceOf(EpisodeNotFoundException.class);
    }

    @Test
    void createCarePlan_episodeHasNoClinic_throws() {
        episode.setAssignedClinic(null);
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)).thenReturn(Optional.of(physician));

        assertThatThrownBy(() -> carePlanService.createCarePlan(
                new CreateCarePlanRequest(episodeId, physicianId, null), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no assigned clinic");
    }

    // ── updateCarePlan ──────────────────────────────────────────────────────────

    @Test
    void updateCarePlan_completed_resolvesEpisode() {
        CarePlan cp = sampleCarePlan();
        when(carePlanRepository.findById(cp.getId())).thenReturn(Optional.of(cp));
        when(carePlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(cp.getId())).thenReturn(List.of());

        carePlanService.updateCarePlan(cp.getId(),
                new UpdateCarePlanRequest(CarePlanStatus.COMPLETED, null), "127.0.0.1");

        assertThat(cp.getStatus()).isEqualTo(CarePlanStatus.COMPLETED);
        assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.RESOLVED);
        verify(episodeRepository).save(episode);
    }

    @Test
    void updateCarePlan_notesOnly_doesNotTouchEpisode() {
        CarePlan cp = sampleCarePlan();
        when(carePlanRepository.findById(cp.getId())).thenReturn(Optional.of(cp));
        when(carePlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(cp.getId())).thenReturn(List.of());

        carePlanService.updateCarePlan(cp.getId(),
                new UpdateCarePlanRequest(null, "Updated notes"), "127.0.0.1");

        assertThat(cp.getNotes()).isEqualTo("Updated notes");
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void getCarePlan_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(carePlanRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> carePlanService.getCarePlan(id))
                .isInstanceOf(CarePlanNotFoundException.class);
    }

    // ── follow-ups ──────────────────────────────────────────────────────────────

    @Test
    void addFollowUp_physicianMessage() {
        CarePlan cp = sampleCarePlan();
        AddFollowUpRequest request = new AddFollowUpRequest(physicianId, false, "How are you feeling?");
        when(carePlanRepository.findById(cp.getId())).thenReturn(Optional.of(cp));
        when(entityManager.getReference(eq(User.class), eq(physicianId))).thenReturn(physician);

        FollowUp saved = FollowUp.builder()
                .id(UUID.randomUUID()).carePlan(cp).initiatedBy(physician)
                .system(false).message("How are you feeling?").createdAt(OffsetDateTime.now()).build();
        when(followUpRepository.save(any())).thenReturn(saved);

        FollowUpResponse response = carePlanService.addFollowUp(cp.getId(), request, "127.0.0.1");

        assertThat(response.message()).isEqualTo("How are you feeling?");
        assertThat(response.system()).isFalse();
        assertThat(response.initiatedBy()).isEqualTo(physicianId);
    }

    @Test
    void addFollowUp_systemPrompt_noInitiator() {
        CarePlan cp = sampleCarePlan();
        AddFollowUpRequest request = new AddFollowUpRequest(null, true, "Daily symptom check-in");
        when(carePlanRepository.findById(cp.getId())).thenReturn(Optional.of(cp));

        FollowUp saved = FollowUp.builder()
                .id(UUID.randomUUID()).carePlan(cp).system(true)
                .message("Daily symptom check-in").createdAt(OffsetDateTime.now()).build();
        when(followUpRepository.save(any())).thenReturn(saved);

        FollowUpResponse response = carePlanService.addFollowUp(cp.getId(), request, null);

        assertThat(response.system()).isTrue();
        assertThat(response.initiatedBy()).isNull();
        verify(entityManager, never()).getReference(eq(User.class), any());
    }

    @Test
    void respondToFollowUp_setsResponseAndResponder() {
        CarePlan cp = sampleCarePlan();
        UUID followUpId = UUID.randomUUID();
        UUID patientId = patient.getId();
        FollowUp followUp = FollowUp.builder()
                .id(followUpId).carePlan(cp).initiatedBy(physician)
                .system(false).message("How are you feeling?").createdAt(OffsetDateTime.now()).build();

        when(followUpRepository.findByIdAndCarePlanId(followUpId, cp.getId())).thenReturn(Optional.of(followUp));
        when(entityManager.getReference(eq(User.class), eq(patientId))).thenReturn(patient);
        when(followUpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FollowUpResponse response = carePlanService.respondToFollowUp(cp.getId(), followUpId,
                new RespondFollowUpRequest(patientId, "Much better, thanks"), "127.0.0.1");

        assertThat(response.response()).isEqualTo("Much better, thanks");
        assertThat(response.respondedBy()).isEqualTo(patientId);
        assertThat(response.respondedAt()).isNotNull();
    }

    @Test
    void respondToFollowUp_notFound_throws() {
        UUID carePlanId = UUID.randomUUID();
        UUID followUpId = UUID.randomUUID();
        when(followUpRepository.findByIdAndCarePlanId(followUpId, carePlanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carePlanService.respondToFollowUp(carePlanId, followUpId,
                new RespondFollowUpRequest(UUID.randomUUID(), "response"), null))
                .isInstanceOf(CarePlanNotFoundException.class);
    }
}
