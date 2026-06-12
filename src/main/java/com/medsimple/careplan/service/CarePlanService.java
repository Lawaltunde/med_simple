package com.medsimple.careplan.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.careplan.dto.*;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.entity.CarePlanStatus;
import com.medsimple.careplan.entity.FollowUp;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.careplan.repository.FollowUpRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanService {

    private final CarePlanRepository carePlanRepository;
    private final FollowUpRepository followUpRepository;
    private final SymptomEpisodeRepository episodeRepository;
    private final PatientRepository userRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;

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
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public CarePlanResponse getCarePlan(UUID id) {
        CarePlan carePlan = findCarePlanById(id);
        return toResponse(carePlan, followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(id));
    }

    @Transactional(readOnly = true)
    public List<CarePlanResponse> getPatientCarePlans(UUID patientId) {
        userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        return carePlanRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(cp -> toResponse(cp, followUpRepository.findByCarePlanIdOrderByCreatedAtAsc(cp.getId())))
                .toList();
    }

    @Transactional
    public CarePlanResponse updateCarePlan(UUID id, UpdateCarePlanRequest request, String ipAddress) {
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

        FollowUp.FollowUpBuilder builder = FollowUp.builder()
                .carePlan(carePlan)
                .system(request.system())
                .message(request.message());

        if (request.initiatedBy() != null) {
            builder.initiatedBy(entityManager.getReference(User.class, request.initiatedBy()));
        }

        FollowUp saved = followUpRepository.save(builder.build());
        auditService.log(request.initiatedBy(), "FOLLOW_UP_ADDED", "follow_ups", saved.getId(), ipAddress);
        return toFollowUpResponse(saved);
    }

    @Transactional
    public FollowUpResponse respondToFollowUp(UUID carePlanId, UUID followUpId,
                                              RespondFollowUpRequest request, String ipAddress) {
        FollowUp followUp = followUpRepository.findByIdAndCarePlanId(followUpId, carePlanId)
                .orElseThrow(() -> new CarePlanNotFoundException(carePlanId));

        followUp.setResponse(request.response());
        followUp.setRespondedBy(entityManager.getReference(User.class, request.respondedBy()));
        followUp.setRespondedAt(OffsetDateTime.now());

        FollowUp saved = followUpRepository.save(followUp);
        auditService.log(request.respondedBy(), "FOLLOW_UP_RESPONDED", "follow_ups", followUpId, ipAddress);
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
