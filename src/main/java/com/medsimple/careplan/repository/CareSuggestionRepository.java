package com.medsimple.careplan.repository;

import com.medsimple.careplan.entity.CareSuggestion;
import com.medsimple.careplan.entity.CareSuggestionStatus;
import com.medsimple.careplan.repository.projection.ApprovedPlanExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareSuggestionRepository extends JpaRepository<CareSuggestion, UUID> {

    // The current PROPOSED suggestion a physician would review for an episode (latest wins).
    Optional<CareSuggestion> findFirstByEpisodeIdAndStatusOrderByCreatedAtDesc(
            UUID episodeId, CareSuggestionStatus status);

    // RAG corpus: physician-approved plans (the signed care_plans.notes) paired with the
    // episode's AI summary used as the matching signal, newest first. Native because
    // care_plan_id is an unmapped FK column. Consent is filtered downstream (per patient).
    @Query(value = """
            SELECT cs.patient_id AS patientId,
                   cp.notes       AS planNotes,
                   e.ai_summary   AS aiSummary
            FROM care_suggestions cs
            JOIN care_plans cp        ON cp.id = cs.care_plan_id
            JOIN symptom_episodes e   ON e.id = cs.episode_id
            WHERE cs.status IN ('APPROVED', 'EDITED')
              AND cp.notes IS NOT NULL
            ORDER BY cs.reviewed_at DESC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<ApprovedPlanExample> findApprovedExemplars(@Param("limit") int limit);
}
