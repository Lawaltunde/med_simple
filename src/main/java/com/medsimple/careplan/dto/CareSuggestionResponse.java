package com.medsimple.careplan.dto;

import com.medsimple.careplan.entity.CareSuggestionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

// Staff-only view of an AI care suggestion. There is deliberately no patient-facing equivalent.
public record CareSuggestionResponse(
        UUID id,
        UUID episodeId,
        CareSuggestionStatus status,
        String suggestedPlan,
        String rationale,
        String adapter,
        String model,
        OffsetDateTime createdAt
) {}
