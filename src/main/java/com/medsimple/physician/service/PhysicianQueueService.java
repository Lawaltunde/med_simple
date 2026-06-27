package com.medsimple.physician.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.clinic.entity.VerificationStatus;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.clinic.repository.ClinicRepository;
import com.medsimple.clinic.repository.ClinicStaffRepository;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.physician.dto.ConfirmUrgencyRequest;
import com.medsimple.physician.dto.QueueEntryResponse;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.exception.BusinessRuleException;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.entity.UrgencyLevel;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhysicianQueueService {

    // Episodes a physician acts on: triaged but not yet resolved.
    private static final List<EpisodeStatus> QUEUE_STATUSES = List.of(
            EpisodeStatus.AI_ANALYZED,
            EpisodeStatus.EMERGENCY_DISPATCHED,
            EpisodeStatus.ASSIGNED,
            EpisodeStatus.IN_PROGRESS
    );

    // Most urgent first (highest enum ordinal), then oldest first (FIFO within a tier).
    // This is the prioritization the product exists to deliver: clinical urgency over booking time.
    private static final Comparator<SymptomEpisode> QUEUE_ORDER =
            Comparator.comparingInt((SymptomEpisode e) -> effectiveUrgencyRank(e)).reversed()
                    .thenComparing(SymptomEpisode::getCreatedAt);

    private final SymptomEpisodeRepository episodeRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicStaffRepository staffRepository;
    private final PatientRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getQueue(UUID clinicId) {
        if (!clinicRepository.existsById(clinicId)) {
            throw new ClinicNotFoundException(clinicId);
        }
        return episodeRepository.findByAssignedClinicIdAndStatusIn(clinicId, QUEUE_STATUSES)
                .stream()
                .sorted(QUEUE_ORDER)
                .map(this::toQueueEntry)
                .toList();
    }

    @Transactional
    public QueueEntryResponse assignPhysician(UUID episodeId, UUID physicianId, String ipAddress) {
        SymptomEpisode episode = findEpisode(episodeId);
        User physician = userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)
                .orElseThrow(() -> new PatientNotFoundException(physicianId));

        // A physician must be credential-verified before taking a patient. This is the single
        // chokepoint for the clinical loop: prescribing, referring, and ordering all require the
        // episode to be assigned, so gating assignment keeps an unverified physician out entirely.
        requireVerifiedPhysician(physician, episode);

        episode.setAssignedPhysician(physician);
        if (episode.getStatus() == EpisodeStatus.AI_ANALYZED
                || episode.getStatus() == EpisodeStatus.EMERGENCY_DISPATCHED) {
            episode.setStatus(EpisodeStatus.ASSIGNED);
        }
        SymptomEpisode saved = episodeRepository.save(episode);
        auditService.log(physicianId, "EPISODE_ASSIGNED", "symptom_episodes", episodeId, ipAddress);
        return toQueueEntry(saved);
    }

    @Transactional
    public QueueEntryResponse confirmUrgency(UUID episodeId, ConfirmUrgencyRequest request, String ipAddress) {
        SymptomEpisode episode = findEpisode(episodeId);
        User physician = userRepository.findByIdAndRole(request.physicianId(), UserRole.PHYSICIAN)
                .orElseThrow(() -> new PatientNotFoundException(request.physicianId()));

        boolean isOverride = episode.getAiUrgencyLevel() != null
                && episode.getAiUrgencyLevel() != request.confirmedUrgencyLevel();

        episode.setConfirmedUrgencyLevel(request.confirmedUrgencyLevel());
        if (isOverride) {
            episode.setUrgencyOverriddenBy(physician);
            episode.setUrgencyOverrideReason(request.overrideReason());
            episode.setUrgencyOverriddenAt(OffsetDateTime.now());
        }
        if (episode.getStatus() == EpisodeStatus.AI_ANALYZED
                || episode.getStatus() == EpisodeStatus.EMERGENCY_DISPATCHED) {
            episode.setStatus(EpisodeStatus.ASSIGNED);
        }

        SymptomEpisode saved = episodeRepository.save(episode);
        auditService.log(request.physicianId(),
                isOverride ? "URGENCY_OVERRIDDEN" : "URGENCY_CONFIRMED",
                "symptom_episodes", episodeId, ipAddress);
        return toQueueEntry(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SymptomEpisode findEpisode(UUID id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new EpisodeNotFoundException(id));
    }

    // The physician's College registration must be VERIFIED (at the episode's clinic if it has one)
    // before they can take the patient. UNVERIFIED / PENDING / REJECTED are all blocked.
    private void requireVerifiedPhysician(User physician, SymptomEpisode episode) {
        UUID clinicId = episode.getAssignedClinic() != null ? episode.getAssignedClinic().getId() : null;
        boolean verified = staffRepository.findByUserIdAndActiveTrue(physician.getId()).stream()
                .filter(s -> clinicId == null || (s.getClinic() != null && clinicId.equals(s.getClinic().getId())))
                .anyMatch(s -> s.getVerificationStatus() == VerificationStatus.VERIFIED);
        if (!verified) {
            throw new BusinessRuleException(
                    "Physician must be credential-verified before taking patients");
        }
    }

    // Effective urgency = physician-confirmed if present, else AI-assessed.
    // Rank is the enum ordinal (ROUTINE=0 .. EMERGENCY=3); -1 if neither is set.
    private static int effectiveUrgencyRank(SymptomEpisode e) {
        UrgencyLevel effective = e.getConfirmedUrgencyLevel() != null
                ? e.getConfirmedUrgencyLevel()
                : e.getAiUrgencyLevel();
        return effective == null ? -1 : effective.ordinal();
    }

    private QueueEntryResponse toQueueEntry(SymptomEpisode e) {
        UrgencyLevel effective = e.getConfirmedUrgencyLevel() != null
                ? e.getConfirmedUrgencyLevel()
                : e.getAiUrgencyLevel();
        return new QueueEntryResponse(
                e.getId(),
                e.getPatient().getId(),
                e.getPatient().getFirstName(),
                e.getPatient().getLastName(),
                effective,
                e.getAiUrgencyLevel(),
                e.getConfirmedUrgencyLevel(),
                e.getStatus(),
                e.getAiSummary(),
                e.getAssignedPhysician() != null ? e.getAssignedPhysician().getId() : null,
                e.getCreatedAt()
        );
    }
}
