package com.medsimple.referral.service;

import com.medsimple.audit.service.AuditService;
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
    private final SpecialtySuggestionDao specialtySuggestionDao;
    private final ReferralDeliveryResolver deliveryResolver;
    private final AuditService auditService;

    @Transactional
    public ReferralResponse createReferral(CreateReferralRequest request, String ipAddress) {
        CarePlan carePlan = carePlanRepository.findById(request.carePlanId())
                .orElseThrow(() -> new CarePlanNotFoundException(request.carePlanId()));

        // PHI-stripped: only the clinical summary text reaches the AI.
        SpecialtySuggestion suggestion = specialtySuggestionDao.suggest(request.clinicalSummary());

        Referral referral = Referral.builder()
                .carePlan(carePlan)
                .patient(carePlan.getPatient())
                .referringPhysician(carePlan.getPhysician())
                .aiSuggestedSpecialty(suggestion.specialty())
                .clinicalSummary(request.clinicalSummary())
                .urgency(request.urgency())
                .status(ReferralStatus.DRAFT)
                .build();

        Referral saved = referralRepository.save(referral);
        auditService.log(carePlan.getPhysician().getId(), "REFERRAL_CREATED",
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
        UUID clinicId = referral.getCarePlan().getClinic().getId();
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
                r.getCarePlan().getId(),
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
