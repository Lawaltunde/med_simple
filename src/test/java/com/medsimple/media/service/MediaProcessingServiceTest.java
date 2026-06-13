package com.medsimple.media.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.media.dao.PhotoAnalysisDao;
import com.medsimple.media.dao.VoiceTranscriptionDao;
import com.medsimple.media.dto.ProcessedSubmissionResponse;
import com.medsimple.media.exception.SubmissionNotFoundException;
import com.medsimple.media.repository.MediaSubmissionRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.InputType;
import com.medsimple.symptom.entity.ProcessingStatus;
import com.medsimple.symptom.entity.SymptomSubmission;
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
class MediaProcessingServiceTest {

    @Mock MediaSubmissionRepository submissionRepository;
    @Mock VoiceTranscriptionDao voiceTranscriptionDao;
    @Mock PhotoAnalysisDao photoAnalysisDao;
    @Mock AuditService auditService;

    @InjectMocks MediaProcessingService service;

    private User patient;

    @BeforeEach
    void setUp() {
        patient = User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@x.com").build();
    }

    private SymptomSubmission submission(InputType type, ProcessingStatus status, String url) {
        return SymptomSubmission.builder()
                .id(UUID.randomUUID()).patient(patient)
                .inputType(type).mediaUrl(url).processingStatus(status)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();
    }

    // ── voice ───────────────────────────────────────────────────────────────────

    @Test
    void process_voice_transcribesAndCompletes() {
        SymptomSubmission s = submission(InputType.VOICE, ProcessingStatus.PENDING, "storage://v.mp3");
        when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(voiceTranscriptionDao.transcribe("storage://v.mp3")).thenReturn("I have a sore throat");
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessedSubmissionResponse response = service.process(s.getId());

        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(response.transcription()).isEqualTo("I have a sore throat");
        verify(photoAnalysisDao, never()).analyze(any());
        verify(auditService).log(patient.getId(), "SUBMISSION_PROCESSED", "symptom_submissions", s.getId(), null);
    }

    // ── photo ───────────────────────────────────────────────────────────────────

    @Test
    void process_photo_analyzesAndCompletes() {
        SymptomSubmission s = submission(InputType.PHOTO, ProcessingStatus.PENDING, "storage://p.jpg");
        when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(photoAnalysisDao.analyze("storage://p.jpg")).thenReturn("Erythematous rash on the left forearm");
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessedSubmissionResponse response = service.process(s.getId());

        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(response.aiAnalysisResult()).isEqualTo("Erythematous rash on the left forearm");
        verify(voiceTranscriptionDao, never()).transcribe(any());
    }

    // ── failure ─────────────────────────────────────────────────────────────────

    @Test
    void process_adapterThrows_marksFailed() {
        SymptomSubmission s = submission(InputType.VOICE, ProcessingStatus.PENDING, "storage://v.mp3");
        when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(voiceTranscriptionDao.transcribe(any())).thenThrow(new RuntimeException("provider down"));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessedSubmissionResponse response = service.process(s.getId());

        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(response.transcription()).isNull();
    }

    // ── idempotency / guards ────────────────────────────────────────────────────

    @Test
    void process_alreadyCompleted_isNoOp() {
        SymptomSubmission s = submission(InputType.VOICE, ProcessingStatus.COMPLETED, "storage://v.mp3");
        when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));

        ProcessedSubmissionResponse response = service.process(s.getId());

        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        verify(voiceTranscriptionDao, never()).transcribe(any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void process_textSubmission_isSkipped() {
        SymptomSubmission s = submission(InputType.TEXT, ProcessingStatus.PENDING, null);
        when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));

        service.process(s.getId());

        verifyNoInteractions(voiceTranscriptionDao, photoAnalysisDao);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void process_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(submissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(id)).isInstanceOf(SubmissionNotFoundException.class);
    }

    // ── poller source ───────────────────────────────────────────────────────────

    @Test
    void findPendingSubmissionIds_queriesVoiceAndPhotoPending() {
        SymptomSubmission a = submission(InputType.VOICE, ProcessingStatus.PENDING, "v");
        SymptomSubmission b = submission(InputType.PHOTO, ProcessingStatus.PENDING, "p");
        when(submissionRepository.findByProcessingStatusAndInputTypeIn(
                eq(ProcessingStatus.PENDING), argThat(types ->
                        types.contains(InputType.VOICE) && types.contains(InputType.PHOTO)
                                && !types.contains(InputType.TEXT))))
                .thenReturn(List.of(a, b));

        List<UUID> ids = service.findPendingSubmissionIds();

        assertThat(ids).containsExactly(a.getId(), b.getId());
    }
}
