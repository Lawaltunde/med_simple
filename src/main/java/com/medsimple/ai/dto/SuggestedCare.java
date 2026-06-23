package com.medsimple.ai.dto;

// The AI's advisory plan of care, produced at triage time alongside urgency.
// Advisory only: a physician must edit/approve before it ever reaches the patient.
// Null when no plan is proposed (fallback adapter, AI failure, or EMERGENCY suppression).
public record SuggestedCare(String plan, String rationale) {}
