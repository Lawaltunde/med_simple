package com.medsimple.physician.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.clinic.repository.ClinicRepository;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.physician.dto.ConfirmUrgencyRequest;
import com.medsimple.physician.dto.QueueEntryResponse;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.entity.UrgencyLevel;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
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
class PhysicianQueueServiceTest {

    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock ClinicRepository clinicRepository;
    @Mock PatientRepository userRepository;
    @Mock AuditService auditService;

    @InjectMocks PhysicianQueueService queueService;

    private UUID clinicId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
    }

    private SymptomEpisode episode(UrgencyLevel ai, UrgencyLevel confirmed,
                                   OffsetDateTime createdAt, String firstName) {
        User patient = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.PATIENT)
                .firstName(firstName).lastName("Doe")
                .email(firstName + "@x.com")
                .createdAt(createdAt).build();
        return SymptomEpisode.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(EpisodeStatus.AI_ANALYZED)
                .aiUrgencyLevel(ai)
                .confirmedUrgencyLevel(confirmed)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    // ── Queue ordering — the core product behaviour ────────────────────────────

    @Test
    void getQueue_ordersByUrgencyNotByTime() {
        OffsetDateTime t0 = OffsetDateTime.now();
        // Created oldest-to-newest, but urgency is the reverse — urgency must win.
        SymptomEpisode routine   = episode(UrgencyLevel.ROUTINE,   null, t0,                 "Routine");
        SymptomEpisode urgent    = episode(UrgencyLevel.URGENT,    null, t0.plusMinutes(1),  "Urgent");
        SymptomEpisode emergency = episode(UrgencyLevel.EMERGENCY, null, t0.plusMinutes(2),  "Emergency");
        SymptomEpisode seeSoon   = episode(UrgencyLevel.SEE_SOON,  null, t0.plusMinutes(3),  "SeeSoon");

        when(clinicRepository.existsById(clinicId)).thenReturn(true);
        when(episodeRepository.findByAssignedClinicIdAndStatusIn(eq(clinicId), anyList()))
                .thenReturn(List.of(routine, urgent, emergency, seeSoon));

        List<QueueEntryResponse> queue = queueService.getQueue(clinicId);

        assertThat(queue).extracting(QueueEntryResponse::effectiveUrgency)
                .containsExactly(UrgencyLevel.EMERGENCY, UrgencyLevel.URGENT,
                        UrgencyLevel.SEE_SOON, UrgencyLevel.ROUTINE);
    }

    @Test
    void getQueue_sameUrgency_oldestFirst() {
        OffsetDateTime t0 = OffsetDateTime.now();
        SymptomEpisode newer = episode(UrgencyLevel.URGENT, null, t0.plusMinutes(10), "Newer");
        SymptomEpisode older = episode(UrgencyLevel.URGENT, null, t0,                 "Older");

        when(clinicRepository.existsById(clinicId)).thenReturn(true);
        when(episodeRepository.findByAssignedClinicIdAndStatusIn(eq(clinicId), anyList()))
                .thenReturn(List.of(newer, older));

        List<QueueEntryResponse> queue = queueService.getQueue(clinicId);

        assertThat(queue).extracting(QueueEntryResponse::patientFirstName)
                .containsExactly("Older", "Newer");
    }

    @Test
    void getQueue_confirmedUrgencyOverridesAiForOrdering() {
        OffsetDateTime t0 = OffsetDateTime.now();
        // AI said ROUTINE, but physician confirmed EMERGENCY — it must jump to the top.
        SymptomEpisode overridden = episode(UrgencyLevel.ROUTINE, UrgencyLevel.EMERGENCY, t0, "Overridden");
        SymptomEpisode aiUrgent   = episode(UrgencyLevel.URGENT,  null,                   t0, "AiUrgent");

        when(clinicRepository.existsById(clinicId)).thenReturn(true);
        when(episodeRepository.findByAssignedClinicIdAndStatusIn(eq(clinicId), anyList()))
                .thenReturn(List.of(aiUrgent, overridden));

        List<QueueEntryResponse> queue = queueService.getQueue(clinicId);

        assertThat(queue.get(0).patientFirstName()).isEqualTo("Overridden");
        assertThat(queue.get(0).effectiveUrgency()).isEqualTo(UrgencyLevel.EMERGENCY);
    }

    @Test
    void getQueue_clinicNotFound_throws() {
        when(clinicRepository.existsById(clinicId)).thenReturn(false);

        assertThatThrownBy(() -> queueService.getQueue(clinicId))
                .isInstanceOf(ClinicNotFoundException.class);
    }

    // ── assignPhysician ─────────────────────────────────────────────────────────

    @Test
    void assignPhysician_setsPhysicianAndMovesToAssigned() {
        UUID episodeId = UUID.randomUUID();
        UUID physicianId = UUID.randomUUID();
        SymptomEpisode ep = episode(UrgencyLevel.URGENT, null, OffsetDateTime.now(), "P");
        ep.setStatus(EpisodeStatus.AI_ANALYZED);
        User physician = User.builder().id(physicianId).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("Smith").email("doc@x.com").build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(ep));
        when(userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)).thenReturn(Optional.of(physician));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QueueEntryResponse response = queueService.assignPhysician(episodeId, physicianId, "127.0.0.1");

        assertThat(response.assignedPhysicianId()).isEqualTo(physicianId);
        assertThat(response.status()).isEqualTo(EpisodeStatus.ASSIGNED);
        verify(auditService).log(physicianId, "EPISODE_ASSIGNED", "symptom_episodes", episodeId, "127.0.0.1");
    }

    // ── confirmUrgency ──────────────────────────────────────────────────────────

    @Test
    void confirmUrgency_matchingAi_noOverrideRecorded() {
        UUID episodeId = UUID.randomUUID();
        UUID physicianId = UUID.randomUUID();
        SymptomEpisode ep = episode(UrgencyLevel.URGENT, null, OffsetDateTime.now(), "P");
        User physician = User.builder().id(physicianId).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("Smith").email("doc@x.com").build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(ep));
        when(userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)).thenReturn(Optional.of(physician));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        queueService.confirmUrgency(episodeId,
                new ConfirmUrgencyRequest(physicianId, UrgencyLevel.URGENT, null), "127.0.0.1");

        assertThat(ep.getConfirmedUrgencyLevel()).isEqualTo(UrgencyLevel.URGENT);
        assertThat(ep.getUrgencyOverriddenBy()).isNull();
        verify(auditService).log(physicianId, "URGENCY_CONFIRMED", "symptom_episodes", episodeId, "127.0.0.1");
    }

    @Test
    void confirmUrgency_differsFromAi_recordsOverride() {
        UUID episodeId = UUID.randomUUID();
        UUID physicianId = UUID.randomUUID();
        SymptomEpisode ep = episode(UrgencyLevel.ROUTINE, null, OffsetDateTime.now(), "P");
        User physician = User.builder().id(physicianId).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("Smith").email("doc@x.com").build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(ep));
        when(userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)).thenReturn(Optional.of(physician));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        queueService.confirmUrgency(episodeId,
                new ConfirmUrgencyRequest(physicianId, UrgencyLevel.EMERGENCY, "Red flags missed by AI"),
                "127.0.0.1");

        assertThat(ep.getConfirmedUrgencyLevel()).isEqualTo(UrgencyLevel.EMERGENCY);
        assertThat(ep.getUrgencyOverriddenBy()).isEqualTo(physician);
        assertThat(ep.getUrgencyOverrideReason()).isEqualTo("Red flags missed by AI");
        assertThat(ep.getUrgencyOverriddenAt()).isNotNull();
        verify(auditService).log(physicianId, "URGENCY_OVERRIDDEN", "symptom_episodes", episodeId, "127.0.0.1");
    }
}
