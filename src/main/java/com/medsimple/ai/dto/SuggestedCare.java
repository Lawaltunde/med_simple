package com.medsimple.ai.dto;

// The AI's advisory plan of care, produced at triage time alongside urgency.
// Advisory only: a physician must edit/approve before it ever reaches the patient.
// Null when no plan is proposed (fallback adapter, AI failure, or EMERGENCY suppression).
// followUpDays: the AI's suggested follow-up interval in days (nullable) — the physician
// approves it into an actual appointment.
public record SuggestedCare(String plan, String rationale, Integer followUpDays) {

    public SuggestedCare(String plan, String rationale) {
        this(plan, rationale, null);
    }
}
