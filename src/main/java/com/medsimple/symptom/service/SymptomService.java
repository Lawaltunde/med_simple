package com.medsimple.symptom.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.patient.entity.PatientClinicRegistration;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.PatientClinicRegistrationRepository;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.dto.*;
import com.medsimple.symptom.entity.*;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import com.medsimple.symptom.repository.SymptomSubmissionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private final SymptomEpisodeRepository episodeRepository;
    private final SymptomSubmissionRepository submissionRepository;
    private final PatientRepository patientRepository;
    private final PatientClinicRegistrationRepository registrationRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;

    @Transactional
    public EpisodeResponse startEpisode(UUID patientId, StartEpisodeRequest request, String ipAddress) {
        User patient = patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        SymptomEpisode.SymptomEpisodeBuilder builder = SymptomEpisode.builder().patient(patient);

        // Routing: use the explicit clinicId if given, otherwise auto-route to the
        // patient's active clinic registration so the episode lands in a physician queue.
        UUID targetClinicId = request.clinicId();
        if (targetClinicId == null) {
            targetClinicId = registrationRepository.findByPatientId(patientId).stream()
                    .filter(PatientClinicRegistration::isActive)
                    .map(reg -> reg.getClinic().getId())
                    .findFirst()
                    .orElse(null);
        }
        if (targetClinicId != null) {
            builder.assignedClinic(entityManager.getReference(Clinic.class, targetClinicId));
        }

        SymptomEpisode saved = episodeRepository.save(builder.build());
        auditService.log(patientId, "EPISODE_STARTED", "symptom_episodes", saved.getId(), ipAddress);

        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<EpisodeResponse> getPatientEpisodes(UUID patientId) {
        patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        return episodeRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(e -> toResponse(e, submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EpisodeResponse getEpisode(UUID episodeId) {
        SymptomEpisode episode = findEpisodeById(episodeId);
        List<SymptomSubmission> submissions = submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId);
        return toResponse(episode, submissions);
    }

    @Transactional
    public SubmissionResponse addSubmission(UUID episodeId, AddSubmissionRequest request, String ipAddress) {
        SymptomEpisode episode = findEpisodeById(episodeId);

        SymptomSubmission submission = SymptomSubmission.builder()
                .episode(episode)
                .patient(episode.getPatient())
                .inputType(request.inputType())
                .textContent(request.textContent())
                .mediaUrl(request.mediaUrl())
                .processingStatus(request.inputType() == InputType.TEXT
                        ? ProcessingStatus.COMPLETED
                        : ProcessingStatus.PENDING)
                .build();

        SymptomSubmission saved = submissionRepository.save(submission);
        auditService.log(episode.getPatient().getId(), "SUBMISSION_ADDED",
                "symptom_submissions", saved.getId(), ipAddress);

        return toSubmissionResponse(saved);
    }

    @Transactional
    public SubmissionResponse updateSubmissionStatus(UUID episodeId, UUID submissionId,
                                                      UpdateSubmissionRequest request) {
        SymptomSubmission submission = submissionRepository.findByIdAndEpisodeId(submissionId, episodeId)
                .orElseThrow(() -> new EpisodeNotFoundException(episodeId));

        submission.setProcessingStatus(request.processingStatus());
        if (request.transcription() != null) submission.setTranscription(request.transcription());
        if (request.aiAnalysisResult() != null) submission.setAiAnalysisResult(request.aiAnalysisResult());

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SymptomEpisode findEpisodeById(UUID id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new EpisodeNotFoundException(id));
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
                submissions.stream().map(this::toSubmissionResponse).toList()
        );
    }

    private SubmissionResponse toSubmissionResponse(SymptomSubmission s) {
        return new SubmissionResponse(
                s.getId(),
                s.getInputType(),
                s.getTextContent(),
                s.getMediaUrl(),
                s.getTranscription(),
                s.getAiAnalysisResult(),
                s.getProcessingStatus(),
                s.getSubmittedAt()
        );
    }
}
