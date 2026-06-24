package com.medsimple.referral.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.referral.dao.*;
import com.medsimple.referral.dto.*;
import com.medsimple.referral.entity.Referral;
import com.medsimple.referral.entity.ReferralStatus;
import com.medsimple.referral.exception.InvalidReferralStateException;
import com.medsimple.referral.exception.ReferralNotFoundException;
import com.medsimple.referral.repository.ReferralRepository;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final CarePlanRepository carePlanRepository;
    private final SymptomEpisodeRepository episodeRepository;
    private final SpecialtySuggestionDao specialtySuggestionDao;
    private final ReferralDeliveryResolver deliveryResolver;
    private final AuditService auditService;
    private final AccessGuard accessGuard;

    @Transactional
    public ReferralResponse createReferral(CreateReferralRequest request, String ipAddress) {
        // Referrals hang off the episode (the encounter), not the care plan. The physician must
        // have taken the episode (assigned to them) first.
        SymptomEpisode episode = episodeRepository.findById(request.episodeId())
                .orElseThrow(() -> new EpisodeNotFoundException(request.episodeId()));
        if (episode.getAssignedClinic() == null || episode.getAssignedPhysician() == null) {
            throw new IllegalStateException(
                    "Episode " + request.episodeId() + " must be assigned to a physician before referring");
        }

        // PHI-stripped: only the clinical summary text reaches the AI.
        SpecialtySuggestion suggestion = specialtySuggestionDao.suggest(request.clinicalSummary());

        Referral referral = Referral.builder()
                .episode(episode)
                .patient(episode.getPatient())
                .referringPhysician(episode.getAssignedPhysician())
                .aiSuggestedSpecialty(suggestion.specialty())
                .clinicalSummary(request.clinicalSummary())
                .urgency(request.urgency())
                .status(ReferralStatus.DRAFT)
                .build();

        Referral saved = referralRepository.save(referral);
        auditService.log(episode.getAssignedPhysician().getId(), "REFERRAL_CREATED",
                "referrals", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReferralResponse getReferral(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getCarePlanReferrals(UUID carePlanId) {
        return referralRepository.findByCarePlanIdOrderByCreatedAtDesc(carePlanId)
                .stream().map(this::toResponse).toList();
    }

    // Staff-facing: the referrals made for an episode (the console's referral list).
    @Transactional(readOnly = true)
    public List<ReferralResponse> getEpisodeReferrals(UUID episodeId) {
        accessGuard.requireStaff();
        return referralRepository.findByEpisodeIdOrderByCreatedAtDesc(episodeId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getPatientReferrals(UUID patientId) {
        // Patient-scoped read: a patient may see only their own referrals; staff any.
        // Same IDOR gate as the symptom/prescription/diagnostic patient endpoints.
        accessGuard.requirePatientSelfOrStaff(patientId);
        return referralRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReferralResponse confirmSpecialty(UUID id, ConfirmSpecialtyRequest request, String ipAddress) {
        Referral referral = findById(id);
        if (referral.getStatus() != ReferralStatus.DRAFT) {
            throw new InvalidReferralStateException(
                    "Specialty can only be confirmed on a DRAFT referral (was " + referral.getStatus() + ")");
        }
        referral.setConfirmedSpecialty(request.confirmedSpecialty());
        Referral saved = referralRepository.save(referral);
        auditService.log(referral.getReferringPhysician().getId(), "REFERRAL_SPECIALTY_CONFIRMED",
                "referrals", id, ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public ReferralResponse submitReferral(UUID id, String ipAddress) {
        Referral referral = findById(id);
        if (referral.getStatus() != ReferralStatus.DRAFT) {
            throw new InvalidReferralStateException(
                    "Only a DRAFT referral can be submitted (was " + referral.getStatus() + ")");
        }
        // If the physician never confirmed, the AI suggestion becomes the operative specialty.
        if (referral.getConfirmedSpecialty() == null) {
            referral.setConfirmedSpecialty(referral.getAiSuggestedSpecialty());
        }

        // clinic_capabilities decides the channel: Ocean eReferral if enabled, else PDF.
        // Orders hang off the episode (care plan optional since the re-parent), so the clinic comes
        // from the episode's assigned clinic, falling back to the care plan for legacy rows.
        UUID clinicId = referral.getEpisode() != null && referral.getEpisode().getAssignedClinic() != null
                ? referral.getEpisode().getAssignedClinic().getId()
                : referral.getCarePlan().getClinic().getId();
        ReferralDeliveryDao delivery = deliveryResolver.resolve(clinicId);
        ReferralDeliveryResult result = delivery.deliver(referral);

        referral.setStatus(result.status());
        referral.setDeliveryMethod(result.deliveryMethod());
        referral.setExternalReferralId(result.externalReferralId());
        referral.setDocumentUrl(result.documentUrl());
        referral.setSubmittedAt(OffsetDateTime.now());

        Referral saved = referralRepository.save(referral);
        auditService.log(referral.getReferringPhysician().getId(), "REFERRAL_SUBMITTED",
                "referrals", id, ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public ReferralResponse acceptReferral(UUID id, String ipAddress) {
        Referral referral = findById(id);
        if (referral.getStatus() != ReferralStatus.SUBMITTED) {
            throw new InvalidReferralStateException(
                    "Only a SUBMITTED referral can be accepted (was " + referral.getStatus() + ")");
        }
        referral.setStatus(ReferralStatus.ACCEPTED);
        return toResponse(referralRepository.save(referral));
    }

    @Transactional
    public ReferralResponse completeReferral(UUID id, String ipAddress) {
        Referral referral = findById(id);
        if (referral.getStatus() != ReferralStatus.ACCEPTED) {
            throw new InvalidReferralStateException(
                    "Only an ACCEPTED referral can be completed (was " + referral.getStatus() + ")");
        }
        referral.setStatus(ReferralStatus.COMPLETED);
        return toResponse(referralRepository.save(referral));
    }

    @Transactional
    public ReferralResponse cancelReferral(UUID id, String ipAddress) {
        Referral referral = findById(id);
        if (referral.getStatus() == ReferralStatus.COMPLETED) {
            throw new InvalidReferralStateException("A completed referral cannot be cancelled");
        }
        referral.setStatus(ReferralStatus.CANCELLED);
        Referral saved = referralRepository.save(referral);
        auditService.log(referral.getReferringPhysician().getId(), "REFERRAL_CANCELLED",
                "referrals", id, ipAddress);
        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Referral findById(UUID id) {
        return referralRepository.findById(id)
                .orElseThrow(() -> new ReferralNotFoundException(id));
    }

    private ReferralResponse toResponse(Referral r) {
        return new ReferralResponse(
                r.getId(),
                r.getEpisode().getId(),
                r.getCarePlan() != null ? r.getCarePlan().getId() : null,
                r.getPatient().getId(),
                r.getReferringPhysician().getId(),
                r.getAiSuggestedSpecialty(),
                r.getConfirmedSpecialty(),
                r.getClinicalSummary(),
                r.getUrgency(),
                r.getStatus(),
                r.getDeliveryMethod(),
                r.getExternalReferralId(),
                r.getDocumentUrl(),
                r.getSubmittedAt(),
                r.getCreatedAt()
        );
    }
}
