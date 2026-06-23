package com.medsimple.ai.dto;

import com.medsimple.symptom.entity.UrgencyLevel;

public record TriageResult(
        UrgencyLevel urgencyLevel,
        String summary,
        SuggestedCare suggestedCare   // nullable — null means no plan was proposed
) {
    // Convenience for adapters/callers that produce urgency + summary only (no plan):
    // the keyword fallback and the AI-failure path never fabricate a treatment.
    public TriageResult(UrgencyLevel urgencyLevel, String summary) {
        this(urgencyLevel, summary, null);
    }
}
