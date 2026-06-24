package com.medsimple.careplan.dto;

import com.medsimple.careplan.entity.CareSuggestionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Staff-only view of an AI care suggestion. There is deliberately no patient-facing equivalent.
// suggestedReferralSpecialty / suggestedTests are the sanitized, advisory orders the physician
// one-click-prefills into the existing referral / diagnostic create flow.
public record CareSuggestionResponse(
        UUID id,
        UUID episodeId,
        CareSuggestionStatus status,
        String suggestedPlan,
        String rationale,
        Integer suggestedFollowUpDays,
        String suggestedReferralSpecialty,
        List<String> suggestedTests,
        String adapter,
        String model,
        OffsetDateTime createdAt
) {}
