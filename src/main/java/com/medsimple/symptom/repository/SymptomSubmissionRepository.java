package com.medsimple.symptom.repository;

import com.medsimple.symptom.entity.SymptomSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SymptomSubmissionRepository extends JpaRepository<SymptomSubmission, UUID> {

    List<SymptomSubmission> findByEpisodeIdOrderBySubmittedAtAsc(UUID episodeId);

    Optional<SymptomSubmission> findByIdAndEpisodeId(UUID id, UUID episodeId);
}
