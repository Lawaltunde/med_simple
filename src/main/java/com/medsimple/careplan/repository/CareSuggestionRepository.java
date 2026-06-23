package com.medsimple.careplan.repository;

import com.medsimple.careplan.entity.CareSuggestion;
import com.medsimple.careplan.entity.CareSuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CareSuggestionRepository extends JpaRepository<CareSuggestion, UUID> {

    // The current PROPOSED suggestion a physician would review for an episode (latest wins).
    Optional<CareSuggestion> findFirstByEpisodeIdAndStatusOrderByCreatedAtDesc(
            UUID episodeId, CareSuggestionStatus status);
}
