package com.medsimple.patient.validation;

import com.medsimple.patient.entity.CoverageType;
import com.medsimple.patient.exception.InvalidHealthCardException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthCardValidatorTest {

    private final HealthCardValidator validator = new HealthCardValidator();

    // 1234567897 passes Luhn (sum 50). 1234567890 does not (sum 43).
    private static final String VALID_OHIP = "1234567897";
    private static final String BAD_CHECK_DIGIT = "1234567890";

    @Test
    void validOhip_passes_plainAndFormattedAndWithVersionCode() {
        assertThatCode(() -> validator.validate(CoverageType.PROVINCIAL, "ON", VALID_OHIP))
                .doesNotThrowAnyException();
        // Spaces/dashes are normalized away.
        assertThatCode(() -> validator.validate(CoverageType.PROVINCIAL, "ON", "1234-567-897"))
                .doesNotThrowAnyException();
        // Optional 2-letter version code is accepted.
        assertThatCode(() -> validator.validate(CoverageType.PROVINCIAL, "ON", "1234567897XY"))
                .doesNotThrowAnyException();
        // Province match is case-insensitive.
        assertThatCode(() -> validator.validate(CoverageType.PROVINCIAL, "on", VALID_OHIP))
                .doesNotThrowAnyException();
    }

    @Test
    void ohipWithBadCheckDigit_isRejected() {
        assertThatThrownBy(() -> validator.validate(CoverageType.PROVINCIAL, "ON", BAD_CHECK_DIGIT))
                .isInstanceOf(InvalidHealthCardException.class)
                .hasMessageContaining("check digit");
    }

    @Test
    void ohipWrongLengthOrCharacters_isRejected() {
        assertThatThrownBy(() -> validator.validate(CoverageType.PROVINCIAL, "ON", "12345"))
                .isInstanceOf(InvalidHealthCardException.class)
                .hasMessageContaining("10 digits");
        assertThatThrownBy(() -> validator.validate(CoverageType.PROVINCIAL, "ON", "12345678901"))
                .isInstanceOf(InvalidHealthCardException.class);
        assertThatThrownBy(() -> validator.validate(CoverageType.PROVINCIAL, "ON", "ABCDEFGHIJ"))
                .isInstanceOf(InvalidHealthCardException.class);
        // Three-letter "version code" is not a valid OHIP suffix.
        assertThatThrownBy(() -> validator.validate(CoverageType.PROVINCIAL, "ON", "1234567897XYZ"))
                .isInstanceOf(InvalidHealthCardException.class);
    }

    @Test
    void nonOntarioProvincial_isNotOhipChecked() {
        // No published algorithm wired for other provinces yet — must not falsely reject.
        assertThatCode(() -> validator.validate(CoverageType.PROVINCIAL, "BC", "9999-999-999"))
                .doesNotThrowAnyException();
    }

    @Test
    void nonProvincialCoverage_isNotOhipChecked() {
        // A private insurer member id is not an OHIP number; the OHIP rule must not apply.
        assertThatCode(() -> validator.validate(CoverageType.PRIVATE, "ON", "MEMBER-ABC-123"))
                .doesNotThrowAnyException();
    }
}
