package com.medsimple.careplan.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.dto.*;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.entity.CarePlanStatus;
import com.medsimple.careplan.entity.CareSuggestion;
import com.medsimple.careplan.entity.CareSuggestionStatus;
import com.medsimple.careplan.entity.FollowUp;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.exception.CareSuggestionNotFoundException;
import com.medsimple.careplan.exception.InvalidSuggestionStateException;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.careplan.repository.CareSuggestionRepository;
import com.medsimple.careplan.repository.FollowUpRepository;
import com.medsimple.notification.entity.NotificationType;
import com.medsimple.notification.service.NotificationService;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanService {

    private final CarePlanRepository carePlanRepository;
    private final CareSuggestionRepository careSuggestionRepository;
    private final FollowUpRepository followUpRepository;
    private final SymptomEpisodeRepository episodeRepository;
    private final PatientRepository userRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;
    private final NotificationService notificationService;
    private final AccessGuard accessGuard;

    @Transactional
    public CarePlanResponse createCarePlan(CreateCarePlanRequest request, String ipAddress) {
        SymptomEpisode episode = episodeRepository.findById(request.episodeId())
                .orElseThrow(() -> new EpisodeNotFoundException(request.episodeId()));

        User physician = userRepository.findByIdAndRole(request.physicianId(), UserRole.PHYSICIAN)
                .orElseThrow(() -> new PatientNotFoundException(request.physicianId()));

        if (episode.getAssignedClinic() == null) {
            throw new IllegalStateException(
                    "Episode " + request.episodeId() + " has no assigned clinic; cannot create a care plan");
        }

        CarePlan carePlan = CarePlan.builder()
                .episode(episode)
                .patient(episode.getPatient())
                .physician(physician)
                .clinic(episode.getAssignedClinic())
                .notes(request.notes())
                .build();
        CarePlan saved = carePlanRepository.save(carePlan);

        // A care plan means the physician is actively treating — move the episode forward.
        episode.setStatus(EpisodeStatus.IN_PROGRESS);
        episodeRepository.save(episode);

        auditService.log(request.physicianId(), "CARE_PLAN_CREATED", "care_plans", saved.getId(), ipAddress);
        // Close the loop back to the patient — this is the care delivered for their complaint.
        notificationService.notify(episode.getPatient().getId(), NotificationType.CARE_PLAN_UPDATE,
                "Your care plan is ready",
                "A physician has reviewed your symptoms and created a care plan. Tap to read it.");
        return toResponse(saved, List.of());
    }

    // ── AI care suggestion: physician review workflow ──────────────────────────

    // Staff-only. The latest PROPOSED suggestion a physician would review for this episode.
    // There is deliberately no patient-facing equivalent — the gate is structural.
    // Returns the latest in-play suggestion (PROPOSED, or already APPROVED/EDITED) so the console
    // still sees the AI's follow-up interval after the plan is approved. FAILED/REJECTED excluded.
    @Transactional(readOnly = true)
    public Optional<CareSuggestionResponse> getSuggestionForEpisode(UUID episodeId) {
        accessGuard.requireStaff();
        return careSuggestionRepository
                .findFirstByEpisodeIdAndStatusInOrderByCreatedAtDesc(episodeId, List.of(
                        CareSuggestionStatus.PROPOSED, CareSuggestionStatus.APPROVED, CareSuggestionStatus.EDITED))
                .map(this::toSuggestionResponse);
    }

    // Approve (or edit-then-approve) a suggestion into a signed, ACTIVE care plan. This is the
    // ONLY path that turns an AI suggestion into patient-visible content, and it is staff-guarded.
    @Transactional
    public CarePlanResponse createFromSuggestion(UUID suggestionId, ApproveSuggestionRequest request,
                                                 String ipAddress) {
        accessGuard.requireStaff();
        CareSuggestion suggestion = careSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new CareSuggestionNotFoundException(suggestionId));
        if (suggestion.getStatus() != CareSuggestionStatus.PROPOSED) {
            throw new InvalidSuggestionStateException(
                    "Suggestion " + suggestionId + " is " + suggestion.getStatus() + ", not PROPOSED");
        }
        User physician = userRepository.findByIdAndRole(request.physicianId(), UserRole.PHYSICIAN)
                .orElseThrow(() -> new PatientNotFoundException(request.physicianId()));

        SymptomEpisode episode = suggestion.getEpisode();
        if (episode.getAssignedClinic() == null) {
            throw new IllegalStateException(
                    "Episode " + episode.getId() + " has no assigned clinic; cannot create a care plan");
        }

        boolean hasEdit = request.editedNotes() != null && !request.editedNotes().isBlank();
        boolean edited = hasEdit && !request.editedNotes().equals(suggestion.getSuggestedPlan());
        String notes = hasEdit ? request.editedNotes() : suggestion.getSuggestedPlan();

        CarePlan carePlan = CarePlan.builder()
                .episode(episode)
                .patient(episode.getPatient())
                .physician(physician)
                .clinic(episode.getAssignedClinic())
                .notes(notes)
                .sourceSuggestionId(suggestion.getId())
                .build();
        CarePlan saved = carePlanRepository.save(carePlan);

        episode.setStatus(EpisodeStatus.IN_PROGRESS);
        episodeRepository.save(episode);

        suggestion.setStatus(edited ? CareSuggestionStatus.EDITED : CareSuggestionStatus.APPROVED);
        suggestion.setReviewedBy(physician);
        suggestion.setReviewedAt(OffsetDateTime.now());
        suggestion.setCarePlanId(saved.getId());
        careSuggestionRepository.save(suggestion);

        auditService.log(request.physicianId(),
                edited ? "CARE_SUGGESTION_EDITED" : "CARE_SUGGESTION_APPROVED",
                "care_suggestions", suggestion.getId(), ipAddress);
        auditService.log(request.physicianId(), "CARE_PLAN_CREATED", "care_plans", saved.getId(), ipAddress);
        notificationService.notify(episode.getPatient().getId(), NotificationType.CARE_PLAN_UPDATE,
                "Your care plan is ready",
                "A physician has reviewed your symptoms and created a care plan. Tap to read it.");
        return toResponse(saved, List.of());
    }

    // Reject a suggestion: no care plan is created; the physician writes from scratch instead.
    @Transactional
    public void rejectSuggestion(UUID suggestionId, RejectSuggestionRequest request, String ipAddress) {
        accessGuard.requireStaff();
        CareSuggestion suggestion = careSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new CareSuggestionNotFoundException(suggestionId));
        if (suggestion.getStatus() != CareSuggestionStatus.PROPOSED) {
            throw new InvalidSuggestionStateException(
                    "Suggestion " + suggestionId + " is " + suggestion.getStatus() + ", not PROPOSED");
        }
        userRepository.findByIdAndRole(request.physicianId(), UserRole.PHYSICIAN)
                .orElseThrow(() -> new PatientNotFoundException(request.physicianId()));

        suggestion.setStatus(CareSuggestionStatus.REJECTED);
        suggestion.setRejectReason(request.reason());
        suggestion.setReviewedBy(entityManager.getReference(User.class, request.physicianId()));
        suggestion.setReviewedAt(OffsetDateTime.now());
        careSuggestionRepository.save(suggestion);
        auditService.log(request.physicianId(), "CARE_SUGGESTION_REJECTED",
                "care_suggestions", suggestion.getId(), ipAddress);
    }

    private CareSuggestionResponse toSuggestionResponse(CareSuggestion s) {
        return new CareSuggestionResponse(
                s.getId(), s.getEpisode().getId(), s.getStatus(),
                s.getSuggestedPlan(), s.getRationale(), s.getSuggestedFollowUpDays(),
                s.getSuggestedReferralSpecialty(), splitTests(s.getSuggestedTests()),
                s.getAdapter(), s.getModel(), s.getCreatedAt());
    }

    // Suggested tests are stored newline-delimited; expose them to the console as a list. Null/blank
    // storage means no suggested tests (the common case, and every FAILED/blocked row).
    private static List<String> splitTests(String tests) {
        if (tests == null || tests.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(tests.split("\n"))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public CarePlanResponse getCarePlan(UUID id) {
        CarePlan carePlan = findCarePlanById(id);
        accessGuard.requirePatientSelfOrStaff(carePlan.getPatient().getId());
        return toResponse(carePlan, followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(id));
    }

    @Transactional(readOnly = true)
    public List<CarePlanResponse> getPatientCarePlans(UUID patientId) {
        accessGuard.requirePatientSelfOrStaff(patientId);
        userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        return carePlanRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(cp -> toResponse(cp, followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(cp.getId())))
                .toList();
    }

    @Transactional
    public CarePlanResponse updateCarePlan(UUID id, UpdateCarePlanRequest request, String ipAddress) {
        accessGuard.requireStaff(); // editing/closing a plan is a clinician action
        CarePlan carePlan = findCarePlanById(id);

        if (request.notes() != null) carePlan.setNotes(request.notes());
        if (request.status() != null) {
            carePlan.setStatus(request.status());
            // Completing the plan resolves the underlying episode.
            if (request.status() == CarePlanStatus.COMPLETED) {
                SymptomEpisode episode = carePlan.getEpisode();
                episode.setStatus(EpisodeStatus.RESOLVED);
                episodeRepository.save(episode);
            }
        }

        CarePlan saved = carePlanRepository.save(carePlan);
        auditService.log(carePlan.getPhysician().getId(), "CARE_PLAN_UPDATED", "care_plans", id, ipAddress);
        return toResponse(saved, followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public FollowUpResponse addFollowUp(UUID carePlanId, AddFollowUpRequest request, String ipAddress) {
        CarePlan carePlan = findCarePlanById(carePlanId);
        accessGuard.requirePatientSelfOrStaff(carePlan.getPatient().getId());

        FollowUp.FollowUpBuilder builder = FollowUp.builder()
                .carePlan(carePlan)
                .system(request.system())
                .message(request.message());

        if (request.initiatedBy() != null) {
            builder.initiatedBy(entityManager.getReference(User.class, request.initiatedBy()));
        }

        FollowUp saved = followUpRepository.save(builder.build());
        auditService.log(request.initiatedBy(), "FOLLOW_UP_ADDED", "follow_ups", saved.getId(), ipAddress);

        // Notify the other side. A physician message goes to the patient; a patient
        // question goes to the physician; a system prompt nudges the patient.
        UUID patientId = carePlan.getPatient().getId();
        UUID physicianId = carePlan.getPhysician().getId();
        if (request.system() || request.initiatedBy() == null) {
            notificationService.notify(patientId, NotificationType.FOLLOW_UP_PROMPT,
                    "A question about your care", request.message());
        } else {
            boolean fromPhysician = request.initiatedBy().equals(physicianId);
            notificationService.notify(fromPhysician ? patientId : physicianId,
                    NotificationType.FOLLOW_UP_RECEIVED, "New message about your care plan", request.message());
        }
        return toFollowUpResponse(saved);
    }

    @Transactional
    public FollowUpResponse respondToFollowUp(UUID carePlanId, UUID followUpId,
                                              RespondFollowUpRequest request, String ipAddress) {
        FollowUp followUp = followUpRepository.findByIdAndCarePlanId(followUpId, carePlanId)
                .orElseThrow(() -> new CarePlanNotFoundException(carePlanId));
        accessGuard.requirePatientSelfOrStaff(followUp.getCarePlan().getPatient().getId());

        followUp.setResponse(request.response());
        followUp.setRespondedBy(entityManager.getReference(User.class, request.respondedBy()));
        followUp.setRespondedAt(OffsetDateTime.now());

        FollowUp saved = followUpRepository.save(followUp);
        auditService.log(request.respondedBy(), "FOLLOW_UP_RESPONDED", "follow_ups", followUpId, ipAddress);

        // Tell whoever asked that they have a reply (fall back to the patient for system prompts).
        UUID recipient = followUp.getInitiatedBy() != null
                ? followUp.getInitiatedBy().getId()
                : followUp.getCarePlan().getPatient().getId();
        notificationService.notify(recipient, NotificationType.FOLLOW_UP_RECEIVED,
                "Reply to your message", request.response());
        return toFollowUpResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CarePlan findCarePlanById(UUID id) {
        return carePlanRepository.findById(id)
                .orElseThrow(() -> new CarePlanNotFoundException(id));
    }

    private CarePlanResponse toResponse(CarePlan cp, List<FollowUp> followUps) {
        return new CarePlanResponse(
                cp.getId(),
                cp.getEpisode().getId(),
                cp.getPatient().getId(),
                cp.getPhysician().getId(),
                cp.getClinic().getId(),
                cp.getStatus(),
                cp.getNotes(),
                cp.getCreatedAt(),
                cp.getUpdatedAt(),
                followUps.stream().map(this::toFollowUpResponse).toList()
        );
    }

    private FollowUpResponse toFollowUpResponse(FollowUp f) {
        return new FollowUpResponse(
                f.getId(),
                f.getInitiatedBy() != null ? f.getInitiatedBy().getId() : null,
                f.isSystem(),
                f.getMessage(),
                f.getResponse(),
                f.getRespondedBy() != null ? f.getRespondedBy().getId() : null,
                f.getRespondedAt(),
                f.getCreatedAt()
        );
    }
}
