package com.medsimple.ai.dto;

import java.util.List;
import java.util.UUID;

// PHI-stripped context sent to the AI model.
// Contains only symptom text and (optionally) physician-approved plan exemplars retrieved from
// consented patients — no patient name, DOB, health card, or identifiers.
public record EpisodeContext(
        UUID episodeId,
        List<String> symptoms,
        List<String> approvedPlanExamples
) {
    // Back-compat for callers that don't supply retrieval exemplars.
    public EpisodeContext(UUID episodeId, List<String> symptoms) {
        this(episodeId, symptoms, List.of());
    }
}
