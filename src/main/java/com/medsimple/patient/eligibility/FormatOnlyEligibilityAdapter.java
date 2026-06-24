package com.medsimple.patient.eligibility;

import com.medsimple.patient.entity.CoverageType;
import org.springframework.stereotype.Component;

/**
 * Default eligibility path: the card has already passed format + check-digit validation, but live
 * coverage cannot be confirmed without an authorized Ministry of Health connection. Reports UNKNOWN
 * so the platform records that the live check did not run, without blocking care.
 *
 * <p>Active unless {@code health-card.eligibility.adapter=hcv} selects the MOH adapter.
 */
@Component
public class FormatOnlyEligibilityAdapter implements HealthCardEligibilityDao {

    @Override
    public EligibilityResult check(CoverageType coverageType, String province, String cardNumber) {
        return new EligibilityResult(
                EligibilityStatus.UNKNOWN,
                "FORMAT_ONLY",
                "Card format validated; live Ministry of Health eligibility was not checked.");
    }
}
