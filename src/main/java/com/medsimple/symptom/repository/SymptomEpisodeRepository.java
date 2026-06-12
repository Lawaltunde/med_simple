package com.medsimple.symptom.repository;

import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SymptomEpisodeRepository extends JpaRepository<SymptomEpisode, UUID> {

    List<SymptomEpisode> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<SymptomEpisode> findByAssignedClinicIdAndStatusInOrderByCreatedAtDesc(
            UUID clinicId, List<EpisodeStatus> statuses);
}
