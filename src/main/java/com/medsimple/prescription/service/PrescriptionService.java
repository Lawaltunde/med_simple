package com.medsimple.prescription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.patient.entity.PatientProfile;
import com.medsimple.patient.repository.PatientProfileRepository;
import com.medsimple.prescription.dao.*;
import com.medsimple.prescription.dto.*;
import com.medsimple.prescription.entity.Prescription;
import com.medsimple.prescription.entity.PrescriptionStatus;
import com.medsimple.prescription.exception.InvalidPrescriptionStateException;
import com.medsimple.prescription.exception.PrescriptionNotFoundException;
import com.medsimple.prescription.repository.PrescriptionRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.exception.BusinessRuleException;
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
public class PrescriptionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PrescriptionRepository prescriptionRepository;
    private final CarePlanRepository carePlanRepository;
    private final SymptomEpisodeRepository episodeRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final MedicationSafetyDao medicationSafetyDao;
    private final PrescriptionDeliveryResolver deliveryResolver;
    private final AuditService auditService;
    private final AccessGuard accessGuard;

    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request, String ipAddress) {
        // Prescriptions hang off the episode, not the care plan. The physician must have taken
        // the episode (assigned to them) first.
        SymptomEpisode episode = episodeRepository.findById(request.episodeId())
                .orElseThrow(() -> new EpisodeNotFoundException(request.episodeId()));
        if (episode.getAssignedClinic() == null || episode.getAssignedPhysician() == null) {
            throw new BusinessRuleException(
                    "Episode " + request.episodeId() + " must be assigned to a physician before prescribing");
        }

        // PHI-stripped safety check against the patient's allergies and current medications.
        SafetyCheckResult safety = medicationSafetyDao.check(buildContext(request, episode.getPatient()));

        Prescription prescription = Prescription.builder()
                .episode(episode)
                .patient(episode.getPatient())
                .prescribingPhysician(episode.getAssignedPhysician())
                .medicationName(request.medicationName())
                .medicationCode(request.medicationCode())
                .dosage(request.dosage())
                .frequency(request.frequency())
                .duration(request.duration())
                .refills(request.refills() != null ? request.refills() : 0)
                .instructions(request.instructions())
                .aiSafetyChecks(serialize(safety))
                .status(PrescriptionStatus.DRAFT)
                .build();

        Prescription saved = prescriptionRepository.save(prescription);
        auditService.log(episode.getAssignedPhysician().getId(), "PRESCRIPTION_CREATED",
                "prescriptions", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescription(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getCarePlanPrescriptions(UUID carePlanId) {
        return prescriptionRepository.findByCarePlanIdOrderByCreatedAtDesc(carePlanId)
                .stream().map(this::toResponse).toList();
    }

    // Staff-facing: the prescriptions written for an episode (the console's prescription list).
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getEpisodePrescriptions(UUID episodeId) {
        accessGuard.requireStaff();
        return prescriptionRepository.findByEpisodeIdOrderByCreatedAtDesc(episodeId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPatientPrescriptions(UUID patientId) {
        // Patient-scoped read: a patient may see only their own; staff may see any.
        // Same IDOR gate as the symptom/care-plan endpoints (enforced in the service layer).
        accessGuard.requirePatientSelfOrStaff(patientId);
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PrescriptionResponse signPrescription(UUID id, String ipAddress) {
        Prescription rx = findById(id);
        if (rx.getStatus() != PrescriptionStatus.DRAFT) {
            throw new InvalidPrescriptionStateException(
                    "Only a DRAFT prescription can be signed (was " + rx.getStatus() + ")");
        }
        rx.setStatus(PrescriptionStatus.SIGNED);
        rx.setSignedAt(OffsetDateTime.now());
        Prescription saved = prescriptionRepository.save(rx);
        auditService.log(rx.getPrescribingPhysician().getId(), "PRESCRIPTION_SIGNED",
                "prescriptions", id, ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse sendPrescription(UUID id, SendPrescriptionRequest request, String ipAddress) {
        Prescription rx = findById(id);
        if (rx.getStatus() != PrescriptionStatus.SIGNED) {
            throw new InvalidPrescriptionStateException(
                    "Only a SIGNED prescription can be sent (was " + rx.getStatus() + ")");
        }

        // clinic_capabilities decides the channel: PrescribeIT if enabled, else PDF fallback.
        // Orders hang off the episode (care plan optional since the re-parent), so the clinic comes
        // from the episode's assigned clinic, falling back to the care plan for legacy rows.
        UUID clinicId = rx.getEpisode() != null && rx.getEpisode().getAssignedClinic() != null
                ? rx.getEpisode().getAssignedClinic().getId()
                : rx.getCarePlan().getClinic().getId();
        PrescriptionDeliveryDao delivery = deliveryResolver.resolve(clinicId);
        DeliveryResult result = delivery.deliver(rx);

        rx.setStatus(result.status());
        rx.setDeliveryMethod(result.deliveryMethod());
        rx.setDocumentUrl(result.documentUrl());
        rx.setPharmacyId(request != null ? request.pharmacyId() : null);
        rx.setSentAt(OffsetDateTime.now());

        Prescription saved = prescriptionRepository.save(rx);
        auditService.log(rx.getPrescribingPhysician().getId(), "PRESCRIPTION_SENT",
                "prescriptions", id, ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse markFilled(UUID id, String ipAddress) {
        Prescription rx = findById(id);
        if (rx.getStatus() != PrescriptionStatus.SENT_ELECTRONIC
                && rx.getStatus() != PrescriptionStatus.SENT_PDF) {
            throw new InvalidPrescriptionStateException(
                    "Only a sent prescription can be marked filled (was " + rx.getStatus() + ")");
        }
        rx.setStatus(PrescriptionStatus.FILLED);
        rx.setFilledAt(OffsetDateTime.now());
        return toResponse(prescriptionRepository.save(rx));
    }

    @Transactional
    public PrescriptionResponse cancelPrescription(UUID id, String ipAddress) {
        Prescription rx = findById(id);
        if (rx.getStatus() == PrescriptionStatus.FILLED) {
            throw new InvalidPrescriptionStateException("A filled prescription cannot be cancelled");
        }
        rx.setStatus(PrescriptionStatus.CANCELLED);
        Prescription saved = prescriptionRepository.save(rx);
        auditService.log(rx.getPrescribingPhysician().getId(), "PRESCRIPTION_CANCELLED",
                "prescriptions", id, ipAddress);
        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Prescription findById(UUID id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
    }

    private MedicationContext buildContext(CreatePrescriptionRequest request, User patient) {
        PatientProfile profile = patientProfileRepository.findByUserId(patient.getId())
                .orElse(null);
        List<String> allergies = profile == null ? List.of()
                : profile.getAllergies().stream().map(a -> a.name()).toList();
        List<String> currentMeds = profile == null ? List.of()
                : profile.getCurrentMedications().stream()
                    .map(m -> m.dose() != null ? m.name() + " " + m.dose() : m.name()).toList();
        return new MedicationContext(
                request.medicationName(), request.dosage(), request.frequency(), currentMeds, allergies);
    }

    private String serialize(SafetyCheckResult safety) {
        try {
            return MAPPER.writeValueAsString(safety);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize safety check result", e);
        }
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return new PrescriptionResponse(
                p.getId(),
                p.getEpisode().getId(),
                p.getCarePlan() != null ? p.getCarePlan().getId() : null,
                p.getPatient().getId(),
                p.getPrescribingPhysician().getId(),
                p.getMedicationName(),
                p.getMedicationCode(),
                p.getDosage(),
                p.getFrequency(),
                p.getDuration(),
                p.getRefills(),
                p.getInstructions(),
                p.getAiSafetyChecks(),
                p.getStatus(),
                p.getDeliveryMethod(),
                p.getPharmacyId(),
                p.getDocumentUrl(),
                p.getSignedAt(),
                p.getSentAt(),
                p.getFilledAt(),
                p.getCreatedAt()
        );
    }
}
