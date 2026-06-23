package com.medsimple.careplan.entity;

public enum CareSuggestionStatus {
    PROPOSED,   // AI produced it, awaiting physician review
    APPROVED,   // physician accepted it unchanged -> became a care plan
    EDITED,     // physician modified it before accepting -> became a care plan
    REJECTED,   // physician rejected it; no care plan created
    FAILED      // suppressed (EMERGENCY), guardrail-blocked, or no AI plan available
}
