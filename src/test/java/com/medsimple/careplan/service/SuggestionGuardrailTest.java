package com.medsimple.careplan.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionGuardrailTest {

    private final SuggestionGuardrail guardrail = new SuggestionGuardrail();

    @ParameterizedTest
    @ValueSource(strings = {
            "Prescribe oxycodone 10mg every 6 hours",
            "Start the patient on alprazolam for anxiety",
            "Give 2 mg of lorazepam as needed",
            "Consider adderall for focus",
            "Begin a course of morphine",
            "Dispense codeine syrup",
            "Administer fentanyl patch",
            "Plan: prescribe amoxicillin 500mg TID",   // 'prescribe' + dose
            "Take ibuprofen 400 mg three times daily"   // specific dose = prescription
    })
    void blocks_dangerousOrPrescribingPlans(String plan) {
        assertThat(guardrail.isBlocked(plan)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Likely viral pharyngitis. Supportive care: rest, fluids, analgesia as needed.",
            "Safety-net advice: return if fever above 39C or difficulty swallowing.",
            "Recommend in-person examination to assess for appendicitis.",
            "Encourage hydration and over-the-counter symptom relief; follow up in 48 hours.",
            "Advise rest and reassessment if symptoms persist past 5 days."
    })
    void allows_cleanPlanOfCare(String plan) {
        assertThat(guardrail.isBlocked(plan)).isFalse();
    }

    @Test
    void nullOrBlank_notBlocked() {
        assertThat(guardrail.isBlocked(null)).isFalse();
        assertThat(guardrail.isBlocked("   ")).isFalse();
    }
}
