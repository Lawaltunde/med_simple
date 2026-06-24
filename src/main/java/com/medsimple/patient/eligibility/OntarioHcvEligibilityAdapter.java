package com.medsimple.patient.eligibility;

import com.medsimple.patient.entity.CoverageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Live Ontario health-card eligibility via the Ministry of Health Health Card Validation (HCV)
 * service. Active when {@code health-card.eligibility.adapter=hcv}.
 *
 * <p>This is the single seam to fill once the clinic is an authorized HCV provider (mirrors the
 * PrescribeIT delivery stub): submit the card number to the MOH HCV endpoint with the provider
 * credentials and map the response to {@link EligibilityStatus}. Until then it returns UNKNOWN with
 * a clear note rather than throwing, so onboarding is never blocked on an unprovisioned integration.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "health-card.eligibility.adapter", havingValue = "hcv")
public class OntarioHcvEligibilityAdapter implements HealthCardEligibilityDao {

    @Override
    public EligibilityResult check(CoverageType coverageType, String province, String cardNumber) {
        // TODO: call the Ontario MOH Health Card Validation service with provider credentials and
        // map the eligibility response once HCV provider access is provisioned.
        log.info("MOH HCV eligibility requested for {} coverage in {} — provider access pending",
                coverageType, province);
        return new EligibilityResult(
                EligibilityStatus.UNKNOWN,
                "MOH_HCV",
                "Ontario MOH HCV eligibility check is awaiting provider onboarding.");
    }
}
