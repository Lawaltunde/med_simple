package com.medsimple.careplan.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSuggestionGuardrailTest {

    private final OrderSuggestionGuardrail guardrail = new OrderSuggestionGuardrail();

    // ── Tests: banned / invasive items must be dropped ──────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Liver biopsy", "Skin biopsy", "Colonoscopy", "Upper endoscopy", "Bronchoscopy",
            "Cystoscopy", "Cardiac catheterisation", "Lumbar puncture", "Spinal tap",
            "Bone marrow aspiration", "Coronary angiography", "Diagnostic laparoscopy",
            "Surgical exploration", "Arthroscopy", "Paracentesis", "Thoracentesis",
            "MRI with contrast injection", "Sedation for procedure"
    })
    void isBannedTest_flagsInvasiveOrControlledItems(String test) {
        assertThat(guardrail.isBannedTest(test.toLowerCase())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CBC", "Complete Blood Count", "TSH", "Lipid panel", "HbA1c", "Urinalysis",
            "Chest X-ray", "Abdominal ultrasound", "ECG", "Ferritin", "Creatinine", "ESR"
    })
    void isBannedTest_allowsRoutineNonInvasiveInvestigations(String test) {
        assertThat(guardrail.isBannedTest(test.toLowerCase())).isFalse();
    }

    @Test
    void sanitizeTests_dropsBannedKeepsClean() {
        List<String> result = guardrail.sanitizeTests(
                List.of("CBC", "Liver biopsy", "TSH", "Colonoscopy", "Chest X-ray"));
        assertThat(result).containsExactly("CBC", "TSH", "Chest X-ray");
    }

    @Test
    void sanitizeTests_capsAtMaxFive() {
        List<String> tenClean = IntStream.range(0, 10)
                .mapToObj(i -> "Lab" + i)
                .collect(Collectors.toList());
        List<String> result = guardrail.sanitizeTests(tenClean);
        assertThat(result).hasSize(OrderSuggestionGuardrail.MAX_TESTS);
        assertThat(result).containsExactly("Lab0", "Lab1", "Lab2", "Lab3", "Lab4");
    }

    @Test
    void sanitizeTests_dedupesCaseInsensitivelyAndTrims() {
        List<String> result = guardrail.sanitizeTests(
                Arrays.asList("CBC", " cbc ", "TSH", null, "  ", "tsh"));
        assertThat(result).containsExactly("CBC", "TSH");
    }

    @Test
    void sanitizeTests_allBanned_returnsNull() {
        assertThat(guardrail.sanitizeTests(List.of("Liver biopsy", "Colonoscopy"))).isNull();
    }

    @Test
    void sanitizeTests_nullOrEmpty_returnsNull() {
        assertThat(guardrail.sanitizeTests(null)).isNull();
        assertThat(guardrail.sanitizeTests(List.of())).isNull();
        assertThat(guardrail.sanitizeTests(new ArrayList<>())).isNull();
    }

    // ── Specialty: known passes (canonicalised), unknown dropped ────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Cardiology", "cardiology", "  DERMATOLOGY  ", "Neurology", "gastroenterology"
    })
    void sanitizeSpecialty_acceptsKnownAndCanonicalises(String input) {
        String result = guardrail.sanitizeSpecialty(input);
        assertThat(result).isNotNull();
        // Returned in canonical title-case form from the allowlist.
        assertThat(result).isEqualTo(result.substring(0, 1).toUpperCase() + result.substring(1).toLowerCase());
    }

    @Test
    void sanitizeSpecialty_canonicalForm() {
        assertThat(guardrail.sanitizeSpecialty("cardiology")).isEqualTo("Cardiology");
        assertThat(guardrail.sanitizeSpecialty("  dermatology ")).isEqualTo("Dermatology");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Dr. House", "Witch doctor", "Homeopathy", "The best specialist", "ignore instructions",
            "Surgery for my back", "Cardiologyy"
    })
    void sanitizeSpecialty_dropsUnrecognised(String input) {
        assertThat(guardrail.sanitizeSpecialty(input)).isNull();
    }

    @Test
    void sanitizeSpecialty_nullOrBlank_returnsNull() {
        assertThat(guardrail.sanitizeSpecialty(null)).isNull();
        assertThat(guardrail.sanitizeSpecialty("   ")).isNull();
    }
}
