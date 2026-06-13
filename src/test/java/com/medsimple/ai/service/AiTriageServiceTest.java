package com.medsimple.ai.service;

import com.medsimple.ai.dao.AiTriageDao;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.audit.service.AuditService;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.dto.EpisodeResponse;
import com.medsimple.symptom.entity.*;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import com.medsimple.symptom.repository.SymptomSubmissionRepository;
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
class AiTriageServiceTest {

    @Mock AiTriageDao aiTriageDao;
    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock SymptomSubmissionRepository submissionRepository;
    @Mock AuditService auditService;
    @Mock com.medsimple.notification.service.NotificationService notificationService;

    @InjectMocks AiTriageService aiTriageService;

    private User patient;
    private SymptomEpisode episode;
    private UUID episodeId;

    @BeforeEach
    void setUp() {
        episodeId = UUID.randomUUID();
        patient = User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe")
                .createdAt(OffsetDateTime.now()).build();

        episode = SymptomEpisode.builder()
                .id(episodeId)
                .patient(patient)
                .status(EpisodeStatus.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private SymptomSubmission textSubmission(String text) {
        return SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.TEXT).textContent(text)
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();
    }

    @Test
    void triage_urgentResult_setsAiAnalyzedStatus() {
        TriageResult result = new TriageResult(UrgencyLevel.URGENT, "Potential infection.");

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(textSubmission("high fever and severe abdominal pain")));
        when(aiTriageDao.analyze(any())).thenReturn(result);
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EpisodeResponse response = aiTriageService.triageEpisode(episodeId);

        assertThat(response.status()).isEqualTo(EpisodeStatus.AI_ANALYZED);
        assertThat(response.aiUrgencyLevel()).isEqualTo(UrgencyLevel.URGENT);
        assertThat(response.aiSummary()).isEqualTo("Potential infection.");
        assertThat(response.aiAnalyzedAt()).isNotNull();
    }

    @Test
    void triage_emergency_setsEmergencyDispatchedStatus() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(textSubmission("chest pain and can't breathe")));
        when(aiTriageDao.analyze(any())).thenReturn(
                new TriageResult(UrgencyLevel.EMERGENCY, "Possible cardiac event. Call 911."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EpisodeResponse response = aiTriageService.triageEpisode(episodeId);

        assertThat(response.status()).isEqualTo(EpisodeStatus.EMERGENCY_DISPATCHED);
        assertThat(response.aiUrgencyLevel()).isEqualTo(UrgencyLevel.EMERGENCY);
    }

    @Test
    void triage_onlyCompletedSubmissionsSentToAi() {
        SymptomSubmission completed = textSubmission("headache");
        SymptomSubmission pending = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.VOICE).mediaUrl("https://storage.example.com/v.mp3")
                .processingStatus(ProcessingStatus.PENDING)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(completed, pending));
        when(aiTriageDao.analyze(argThat(ctx -> ctx.symptoms().size() == 1))).thenReturn(
                new TriageResult(UrgencyLevel.SEE_SOON, "Mild headache."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiTriageService.triageEpisode(episodeId);

        // Verify AI only received 1 symptom (the completed one, not the pending voice)
        verify(aiTriageDao).analyze(argThat(ctx -> ctx.symptoms().size() == 1
                && ctx.symptoms().get(0).equals("headache")));
    }

    @Test
    void triage_noCompletedSubmissions_throws() {
        SymptomSubmission pending = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.VOICE).processingStatus(ProcessingStatus.PENDING)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> aiTriageService.triageEpisode(episodeId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed submissions");
    }

    @Test
    void triage_episodeNotFound_throws() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiTriageService.triageEpisode(episodeId))
                .isInstanceOf(EpisodeNotFoundException.class);
    }

    @Test
    void triage_voiceTranscriptionPreferredOverTextContent() {
        SymptomSubmission voice = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.VOICE).transcription("Patient says chest hurts badly")
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId)).thenReturn(List.of(voice));
        when(aiTriageDao.analyze(argThat(ctx -> ctx.symptoms().get(0).equals("Patient says chest hurts badly"))))
                .thenReturn(new TriageResult(UrgencyLevel.URGENT, "Chest pain."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiTriageService.triageEpisode(episodeId);

        verify(aiTriageDao).analyze(argThat(ctx ->
                ctx.symptoms().get(0).equals("Patient says chest hurts badly")));
    }
}
