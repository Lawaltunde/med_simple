package com.medsimple.patient.eligibility;

/**
 * Result of a health-card eligibility check: the status, the source that produced it
 * (e.g. MOH_HCV, FORMAT_ONLY), and a human-readable note.
 */
public record EligibilityResult(
        EligibilityStatus status,
        String source,
        String message
) {}
