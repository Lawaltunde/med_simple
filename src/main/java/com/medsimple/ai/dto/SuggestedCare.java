package com.medsimple.ai.dto;

import java.util.List;

// The AI's advisory plan of care, produced at triage time alongside urgency.
// Advisory only: a physician must edit/approve before it ever reaches the patient.
// Null when no plan is proposed (fallback adapter, AI failure, or EMERGENCY suppression).
// followUpDays: the AI's suggested follow-up interval in days (nullable) — the physician
//   approves it into an actual appointment.
// referralSpecialty: a standard specialty the AI suggests referring to (nullable) — the physician
//   one-click-prefills the referral create form, then reviews/submits via the existing flow.
// suggestedTests: routine, non-invasive investigations the AI suggests (nullable) — the physician
//   one-click-prefills the diagnostic create form. The deterministic guardrail screens these for
//   invasive / controlled items before they are ever persisted.
public record SuggestedCare(
        String plan,
        String rationale,
        Integer followUpDays,
        String referralSpecialty,
        List<String> suggestedTests) {

    public SuggestedCare(String plan, String rationale) {
        this(plan, rationale, null, null, null);
    }

    public SuggestedCare(String plan, String rationale, Integer followUpDays) {
        this(plan, rationale, followUpDays, null, null);
    }
}
