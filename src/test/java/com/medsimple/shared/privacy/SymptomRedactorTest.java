package com.medsimple.shared.privacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate test for {@link SymptomRedactor}. Deterministic, free, no infrastructure.
 *
 * <p>All test data is synthetic. No real health-card numbers, phones, or emails appear here.
 * Two properties are asserted everywhere: (1) the raw identifier value is GONE from the output,
 * and (2) clinical text is preserved.
 */
class SymptomRedactorTest {

    private final SymptomRedactor redactor = new SymptomRedactor();

    // ── null / blank handling ────────────────────────────────────────────────

    @Test
    void redact_null_returnsNull() {
        assertThat(redactor.redact(null)).isNull();
    }

    @Test
    void redact_blank_returnsBlankUnchanged() {
        assertThat(redactor.redact("")).isEmpty();
        assertThat(redactor.redact("   ")).isEqualTo("   ");
    }

    // ── explicit self-identification ─────────────────────────────────────────

    @Test
    @DisplayName("'my name is X' -> name redacted, lead-in kept")
    void redact_myNameIs() {
        String out = redactor.redact("Hi, my name is Jane Doe and I have a sore throat.");
        assertThat(out).doesNotContain("Jane Doe");
        assertThat(out).contains("my name is [NAME]");
        assertThat(out).contains("sore throat");
    }

    @Test
    void redact_iAm_withName() {
        String out = redactor.redact("I am John Smith, my chest hurts.");
        assertThat(out).doesNotContain("John Smith");
        assertThat(out).contains("[NAME]");
        assertThat(out).contains("chest hurts");
    }

    @Test
    void redact_thisIs_withApostropheName() {
        String out = redactor.redact("This is Mary O'Brien calling about a rash.");
        assertThat(out).doesNotContain("Mary O'Brien");
        assertThat(out).contains("[NAME]");
        assertThat(out).contains("rash");
    }

    @Test
    @DisplayName("'I am a 45 year old' is NOT treated as a name")
    void redact_iAmAge_notAName() {
        String out = redactor.redact("I am a 45 year old with a fever.");
        assertThat(out).isEqualTo("I am a 45 year old with a fever.");
    }

    @Test
    @DisplayName("'I am feeling dizzy' is NOT treated as a name")
    void redact_iAmFeeling_notAName() {
        String out = redactor.redact("I am feeling dizzy and nauseous.");
        assertThat(out).isEqualTo("I am feeling dizzy and nauseous.");
    }

    // ── health-card / OHIP numbers ───────────────────────────────────────────

    @Test
    void redact_healthCard_tenDigits() {
        String out = redactor.redact("My health card is 1234567890 if you need it.");
        assertThat(out).doesNotContain("1234567890");
        assertThat(out).contains("[HEALTH_CARD]");
    }

    @Test
    void redact_healthCard_groupedWithDashes() {
        String out = redactor.redact("OHIP 1234-567-890 AB on file.");
        assertThat(out).doesNotContain("1234-567-890");
        assertThat(out).contains("[HEALTH_CARD]");
    }

    @Test
    void redact_healthCard_withVersionCode() {
        String out = redactor.redact("card number 1234567890AB please.");
        assertThat(out).doesNotContain("1234567890AB");
        assertThat(out).contains("[HEALTH_CARD]");
    }

    // ── phone numbers ────────────────────────────────────────────────────────

    @Test
    void redact_phone_dashed() {
        String out = redactor.redact("Call me back at 416-555-0199 thanks.");
        assertThat(out).doesNotContain("416-555-0199");
        assertThat(out).contains("[PHONE]");
    }

    @Test
    void redact_phone_parensAndSpaces() {
        String out = redactor.redact("reach me on (416) 555 0199 anytime.");
        assertThat(out).doesNotContain("(416) 555 0199");
        assertThat(out).contains("[PHONE]");
    }

    @Test
    void redact_phone_withCountryCode() {
        String out = redactor.redact("my cell is +1 416 555 0199.");
        assertThat(out).doesNotContain("416 555 0199");
        assertThat(out).contains("[PHONE]");
    }

    // ── email ────────────────────────────────────────────────────────────────

    @Test
    void redact_email() {
        String out = redactor.redact("email jane.doe@example.com for results.");
        assertThat(out).doesNotContain("jane.doe@example.com");
        assertThat(out).contains("[EMAIL]");
    }

    // ── postal code ──────────────────────────────────────────────────────────

    @Test
    void redact_postalCode_withSpace() {
        String out = redactor.redact("I live at M5V 3L9 downtown.");
        assertThat(out).doesNotContain("M5V 3L9");
        assertThat(out).contains("[POSTAL_CODE]");
    }

    @Test
    void redact_postalCode_noSpace() {
        String out = redactor.redact("postal code m5v3l9 here.");
        assertThat(out).doesNotContain("m5v3l9");
        assertThat(out).contains("[POSTAL_CODE]");
    }

    // ── clinical text preserved ──────────────────────────────────────────────

    @Test
    @DisplayName("Pure clinical text passes through untouched")
    void redact_clinicalText_unchanged() {
        String clinical = "Sharp stabbing pain in lower right abdomen for 3 days, "
                + "worse after eating, mild fever around 38.2 degrees, no vomiting. "
                + "Took 2 ibuprofen 400mg twice a day with little relief.";
        assertThat(redactor.redact(clinical)).isEqualTo(clinical);
    }

    @Test
    @DisplayName("Dosages and durations are not mistaken for identifiers")
    void redact_dosesAndDurations_unchanged() {
        String s = "blood pressure 120/80, took 500 mg amoxicillin 3 times daily for 7 days.";
        assertThat(redactor.redact(s)).isEqualTo(s);
    }

    @Test
    @DisplayName("Small numbers (counts, temps) survive; only long ID-shaped numbers go")
    void redact_smallNumbers_unchanged() {
        String s = "woke up 4 times last night, temp 39, pain 8 out of 10.";
        assertThat(redactor.redact(s)).isEqualTo(s);
    }

    // ── combined / multiple identifiers in one string ────────────────────────

    @Test
    @DisplayName("Multiple identifiers in one message are all removed")
    void redact_multipleIdentifiers() {
        String s = "Hi my name is Jane Doe, OHIP 1234-567-890, call 416-555-0199 "
                + "or email jane@example.com. I have a migraine.";
        String out = redactor.redact(s);
        assertThat(out)
                .doesNotContain("Jane Doe")
                .doesNotContain("1234-567-890")
                .doesNotContain("416-555-0199")
                .doesNotContain("jane@example.com");
        assertThat(out)
                .contains("[NAME]")
                .contains("[HEALTH_CARD]")
                .contains("[PHONE]")
                .contains("[EMAIL]")
                .contains("migraine");
    }

    @Test
    @DisplayName("Idempotent: redacting already-redacted text changes nothing")
    void redact_idempotent() {
        String once = redactor.redact("my name is Jane Doe, call 416-555-0199.");
        String twice = redactor.redact(once);
        assertThat(twice).isEqualTo(once);
    }
}
