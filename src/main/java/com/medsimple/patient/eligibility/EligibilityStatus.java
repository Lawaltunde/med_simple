package com.medsimple.patient.eligibility;

/**
 * Outcome of a provincial health-card eligibility check.
 *
 * <p>ELIGIBLE — the Ministry of Health confirms active coverage.
 * <p>INELIGIBLE — the card is not active (expired, invalid, out-of-province).
 * <p>UNKNOWN — eligibility could not be confirmed live (e.g. only format was validated, or the
 * MOH service is not yet provisioned). Never block care on UNKNOWN — surface it for follow-up.
 */
public enum EligibilityStatus {
    ELIGIBLE,
    INELIGIBLE,
    UNKNOWN
}
