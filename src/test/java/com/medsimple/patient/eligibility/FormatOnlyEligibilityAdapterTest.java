package com.medsimple.patient.eligibility;

import com.medsimple.patient.entity.CoverageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormatOnlyEligibilityAdapterTest {

    private final HealthCardEligibilityDao dao = new FormatOnlyEligibilityAdapter();

    @Test
    void reportsUnknownWithFormatOnlySource_neverBlocks() {
        EligibilityResult result = dao.check(CoverageType.PROVINCIAL, "ON", "1234567897");

        assertThat(result.status()).isEqualTo(EligibilityStatus.UNKNOWN);
        assertThat(result.source()).isEqualTo("FORMAT_ONLY");
        assertThat(result.message()).containsIgnoringCase("not checked");
    }
}
