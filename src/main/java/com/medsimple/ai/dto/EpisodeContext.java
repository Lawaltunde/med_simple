package com.medsimple.ai.dto;

import java.util.List;
import java.util.UUID;

// PHI-stripped context sent to the AI model.
// Contains only symptom text — no patient name, DOB, health card, or identifiers.
public record EpisodeContext(
        UUID episodeId,
        List<String> symptoms
) {}
