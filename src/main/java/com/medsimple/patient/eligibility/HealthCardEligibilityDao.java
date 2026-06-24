package com.medsimple.patient.eligibility;

import com.medsimple.patient.entity.CoverageType;

/**
 * Confirms a patient's provincial health-card coverage is active with the Ministry of Health.
 * Real-adapter + fallback seam: {@link OntarioHcvEligibilityAdapter} calls the provincial MOH
 * validation service (Ontario HCV) when provider access is provisioned; the default
 * {@link FormatOnlyEligibilityAdapter} reports UNKNOWN after the deterministic format/check-digit
 * pass. Callers depend only on this interface, so the live MOH integration slots in unchanged.
 */
public interface HealthCardEligibilityDao {

    EligibilityResult check(CoverageType coverageType, String province, String cardNumber);
}
