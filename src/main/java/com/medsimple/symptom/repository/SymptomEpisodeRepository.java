package com.medsimple.symptom.repository;

import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SymptomEpisodeRepository extends JpaRepository<SymptomEpisode, UUID> {

    List<SymptomEpisode> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    // Backstop source for the media poller: episodes left in a status (SUBMITTED) past a grace
    // window. Used to triage episodes whose synchronous process→triage chain never completed.
    // Returns ids only — the triage step re-loads and re-checks each under its own transaction.
    @Query("select e.id from SymptomEpisode e where e.status = :status and e.updatedAt < :cutoff")
    List<UUID> findIdsByStatusAndUpdatedAtBefore(
            @Param("status") EpisodeStatus status, @Param("cutoff") OffsetDateTime cutoff);

    List<SymptomEpisode> findByAssignedClinicIdAndStatusInOrderByCreatedAtDesc(
            UUID clinicId, List<EpisodeStatus> statuses);

    // Physician queue source — sorting by clinical urgency happens in the service
    // (PhysicianQueueService) where the comparator is deterministic and unit-testable.
    List<SymptomEpisode> findByAssignedClinicIdAndStatusIn(UUID clinicId, List<EpisodeStatus> statuses);
}
