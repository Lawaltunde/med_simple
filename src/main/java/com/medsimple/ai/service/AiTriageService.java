package com.medsimple.ai.service;

import com.medsimple.ai.dao.AiTriageDao;
import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.symptom.dto.EpisodeResponse;
import com.medsimple.symptom.dto.SubmissionResponse;
import com.medsimple.symptom.entity.*;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import com.medsimple.symptom.repository.SymptomSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTriageService {

    private final AiTriageDao aiTriageDao;
    private final SymptomEpisodeRepository episodeRepository;
    private final SymptomSubmissionRepository submissionRepository;
    private final AuditService auditService;
    private final com.medsimple.notification.service.NotificationService notificationService;
    private final AccessGuard accessGuard;

    @Transactional
    public EpisodeResponse triageEpisode(UUID episodeId) {
        SymptomEpisode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new EpisodeNotFoundException(episodeId));
        accessGuard.requirePatientSelfOrStaff(episode.getPatient().getId());

        List<String> symptoms = collectSymptomTexts(episodeId);
        if (symptoms.isEmpty()) {
            throw new IllegalStateException("No completed submissions found for episode " + episodeId);
        }

        // PHI-stripped context — only symptom text reaches the model
        EpisodeContext context = new EpisodeContext(episodeId, symptoms);
        TriageResult result = aiTriageDao.analyze(context);

        episode.setAiUrgencyLevel(result.urgencyLevel());
        episode.setAiSummary(result.summary());
        episode.setAiAnalyzedAt(OffsetDateTime.now());

        if (result.urgencyLevel() == UrgencyLevel.EMERGENCY) {
            episode.setStatus(EpisodeStatus.EMERGENCY_DISPATCHED);
            log.warn("EMERGENCY urgency for episode {} — immediate action required", episodeId);
        } else {
            episode.setStatus(EpisodeStatus.AI_ANALYZED);
        }

        SymptomEpisode saved = episodeRepository.save(episode);
        auditService.log(episode.getPatient().getId(), "EPISODE_AI_TRIAGED",
                "symptom_episodes", episodeId, null);

        // Notify the patient that their symptoms have been assessed.
        UUID patientId = episode.getPatient().getId();
        if (result.urgencyLevel() == UrgencyLevel.EMERGENCY) {
            notificationService.notify(patientId,
                    com.medsimple.notification.entity.NotificationType.EMERGENCY_ALERT,
                    "Urgent: seek care now",
                    "Your reported symptoms may be an emergency. Please call 911 or go to the nearest emergency department.");
        } else {
            notificationService.notify(patientId,
                    com.medsimple.notification.entity.NotificationType.URGENCY_UPDATED,
                    "Your symptoms have been assessed",
                    "Assessed urgency: " + result.urgencyLevel() + ". " + result.summary());
        }

        List<SymptomSubmission> submissions = submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId);
        return toResponse(saved, submissions);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> collectSymptomTexts(UUID episodeId) {
        return submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId)
                .stream()
                .filter(s -> s.getProcessingStatus() == ProcessingStatus.COMPLETED)
                .map(s -> {
                    // Prefer transcription for voice; fall back to text content
                    if (s.getTranscription() != null && !s.getTranscription().isBlank()) {
                        return s.getTranscription();
                    }
                    return s.getTextContent() != null ? s.getTextContent() : "";
                })
                .filter(text -> !text.isBlank())
                .toList();
    }

    private EpisodeResponse toResponse(SymptomEpisode e, List<SymptomSubmission> submissions) {
        return new EpisodeResponse(
                e.getId(),
                e.getPatient().getId(),
                e.getAssignedClinic() != null ? e.getAssignedClinic().getId() : null,
                e.getAssignedPhysician() != null ? e.getAssignedPhysician().getId() : null,
                e.getStatus(),
                e.getAiUrgencyLevel(),
                e.getAiSummary(),
                e.getAiAnalyzedAt(),
                e.getConfirmedUrgencyLevel(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                submissions.stream().map(s -> new SubmissionResponse(
                        s.getId(), s.getInputType(), s.getTextContent(), s.getMediaUrl(),
                        s.getTranscription(), s.getAiAnalysisResult(),
                        s.getProcessingStatus(), s.getSubmittedAt()
                )).toList()
        );
    }
}
