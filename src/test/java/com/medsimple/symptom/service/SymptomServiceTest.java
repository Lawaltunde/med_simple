package com.medsimple.symptom.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.dto.*;
import com.medsimple.symptom.entity.*;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import com.medsimple.symptom.repository.SymptomSubmissionRepository;
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
class SymptomServiceTest {

    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock SymptomSubmissionRepository submissionRepository;
    @Mock PatientRepository patientRepository;
    @Mock AuditService auditService;
    @Mock EntityManager entityManager;

    @InjectMocks SymptomService symptomService;

    private User patient;
    private UUID patientId;
    private SymptomEpisode sampleEpisode;
    private UUID episodeId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        episodeId = UUID.randomUUID();

        patient = User.builder()
                .id(patientId)
                .email("jane@example.com")
                .role(UserRole.PATIENT)
                .firstName("Jane")
                .lastName("Doe")
                .createdAt(OffsetDateTime.now())
                .build();

        sampleEpisode = SymptomEpisode.builder()
                .id(episodeId)
                .patient(patient)
                .status(EpisodeStatus.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ── startEpisode ──────────────────────────────────────────────────────────

    @Test
    void startEpisode_noClinic_createsSubmittedEpisode() {
        when(patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)).thenReturn(Optional.of(patient));
        when(episodeRepository.save(any())).thenReturn(sampleEpisode);

        EpisodeResponse response = symptomService.startEpisode(patientId, new StartEpisodeRequest(null), "127.0.0.1");

        assertThat(response.status()).isEqualTo(EpisodeStatus.SUBMITTED);
        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.aiUrgencyLevel()).isNull();
        verify(auditService).log(patientId, "EPISODE_STARTED", "symptom_episodes", episodeId, "127.0.0.1");
    }

    @Test
    void startEpisode_patientNotFound_throws() {
        when(patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> symptomService.startEpisode(patientId, new StartEpisodeRequest(null), null))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ── getEpisode ────────────────────────────────────────────────────────────

    @Test
    void getEpisode_found_includesSubmissions() {
        SymptomSubmission sub = SymptomSubmission.builder()
                .id(UUID.randomUUID())
                .episode(sampleEpisode)
                .patient(patient)
                .inputType(InputType.TEXT)
                .textContent("I have a headache")
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(sampleEpisode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId)).thenReturn(List.of(sub));

        EpisodeResponse response = symptomService.getEpisode(episodeId);

        assertThat(response.submissions()).hasSize(1);
        assertThat(response.submissions().get(0).textContent()).isEqualTo("I have a headache");
    }

    @Test
    void getEpisode_notFound_throws() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> symptomService.getEpisode(episodeId))
                .isInstanceOf(EpisodeNotFoundException.class)
                .hasMessageContaining(episodeId.toString());
    }

    // ── addSubmission ─────────────────────────────────────────────────────────

    @Test
    void addTextSubmission_completedImmediately() {
        AddSubmissionRequest request = new AddSubmissionRequest(InputType.TEXT, "Chest pain for 2 hours", null);

        SymptomSubmission saved = SymptomSubmission.builder()
                .id(UUID.randomUUID())
                .episode(sampleEpisode)
                .patient(patient)
                .inputType(InputType.TEXT)
                .textContent("Chest pain for 2 hours")
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(sampleEpisode));
        when(submissionRepository.save(any())).thenReturn(saved);

        SubmissionResponse response = symptomService.addSubmission(episodeId, request, "127.0.0.1");

        assertThat(response.inputType()).isEqualTo(InputType.TEXT);
        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    }

    @Test
    void addVoiceSubmission_pendingUntilAiProcesses() {
        AddSubmissionRequest request = new AddSubmissionRequest(InputType.VOICE, null, "https://storage.example.com/voice.mp3");

        SymptomSubmission saved = SymptomSubmission.builder()
                .id(UUID.randomUUID())
                .episode(sampleEpisode)
                .patient(patient)
                .inputType(InputType.VOICE)
                .mediaUrl("https://storage.example.com/voice.mp3")
                .processingStatus(ProcessingStatus.PENDING)
                .submittedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(sampleEpisode));
        when(submissionRepository.save(any())).thenReturn(saved);

        SubmissionResponse response = symptomService.addSubmission(episodeId, request, null);

        assertThat(response.inputType()).isEqualTo(InputType.VOICE);
        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.PENDING);
    }

    // ── updateSubmissionStatus ────────────────────────────────────────────────

    @Test
    void updateSubmissionStatus_aiCallbackSetsTranscription() {
        UUID submissionId = UUID.randomUUID();
        SymptomSubmission existing = SymptomSubmission.builder()
                .id(submissionId)
                .episode(sampleEpisode)
                .patient(patient)
                .inputType(InputType.VOICE)
                .mediaUrl("https://storage.example.com/voice.mp3")
                .processingStatus(ProcessingStatus.PROCESSING)
                .submittedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        UpdateSubmissionRequest request = new UpdateSubmissionRequest(
                ProcessingStatus.COMPLETED, "Patient reports chest pain", "Potential cardiac symptom");

        when(submissionRepository.findByIdAndEpisodeId(submissionId, episodeId)).thenReturn(Optional.of(existing));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = symptomService.updateSubmissionStatus(episodeId, submissionId, request);

        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(response.transcription()).isEqualTo("Patient reports chest pain");
        assertThat(response.aiAnalysisResult()).isEqualTo("Potential cardiac symptom");
    }
}
