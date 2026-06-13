package com.medsimple.media.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.media.dao.PhotoAnalysisDao;
import com.medsimple.media.dao.VoiceTranscriptionDao;
import com.medsimple.media.dto.ProcessedSubmissionResponse;
import com.medsimple.media.exception.SubmissionNotFoundException;
import com.medsimple.media.repository.MediaSubmissionRepository;
import com.medsimple.symptom.entity.InputType;
import com.medsimple.symptom.entity.ProcessingStatus;
import com.medsimple.symptom.entity.SymptomSubmission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Processes PENDING voice/photo submissions: routes to the transcription or vision adapter,
 * stores the result, and advances the submission to COMPLETED (or FAILED).
 * Each process(id) is its own transaction so the scheduler/worker can drive many independently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingService {

    private static final List<InputType> MEDIA_TYPES = List.of(InputType.VOICE, InputType.PHOTO);

    private final MediaSubmissionRepository submissionRepository;
    private final VoiceTranscriptionDao voiceTranscriptionDao;
    private final PhotoAnalysisDao photoAnalysisDao;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UUID> findPendingSubmissionIds() {
        return submissionRepository
                .findByProcessingStatusAndInputTypeIn(ProcessingStatus.PENDING, MEDIA_TYPES)
                .stream().map(SymptomSubmission::getId).toList();
    }

    @Transactional
    public ProcessedSubmissionResponse process(UUID submissionId) {
        SymptomSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        // Idempotent: only PENDING media submissions are processed.
        if (submission.getProcessingStatus() != ProcessingStatus.PENDING
                || !MEDIA_TYPES.contains(submission.getInputType())) {
            return toResponse(submission);
        }

        try {
            if (submission.getInputType() == InputType.VOICE) {
                submission.setTranscription(voiceTranscriptionDao.transcribe(submission.getMediaUrl()));
            } else { // PHOTO
                submission.setAiAnalysisResult(photoAnalysisDao.analyze(submission.getMediaUrl()));
            }
            submission.setProcessingStatus(ProcessingStatus.COMPLETED);
            auditService.log(submission.getPatient().getId(), "SUBMISSION_PROCESSED",
                    "symptom_submissions", submissionId, null);
        } catch (Exception e) {
            log.error("Media processing failed for submission {}", submissionId, e);
            submission.setProcessingStatus(ProcessingStatus.FAILED);
        }

        return toResponse(submissionRepository.save(submission));
    }

    private ProcessedSubmissionResponse toResponse(SymptomSubmission s) {
        return new ProcessedSubmissionResponse(
                s.getId(), s.getInputType(), s.getMediaUrl(),
                s.getTranscription(), s.getAiAnalysisResult(), s.getProcessingStatus());
    }
}
