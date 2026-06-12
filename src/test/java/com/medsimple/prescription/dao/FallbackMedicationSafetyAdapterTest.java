package com.medsimple.prescription.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackMedicationSafetyAdapterTest {

    private FallbackMedicationSafetyAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FallbackMedicationSafetyAdapter();
    }

    @Test
    void allergyMatch_isFlagged() {
        SafetyCheckResult result = adapter.check(new MedicationContext(
                "Amoxicillin", "500mg", "TID", List.of(), List.of("Amoxicillin")));

        assertThat(result.allergyFlags()).hasSize(1);
        assertThat(result.allergyFlags().get(0)).contains("Amoxicillin");
        assertThat(result.hasFindings()).isTrue();
    }

    @Test
    void knownInteraction_warfarinPlusAspirin_isFlagged() {
        SafetyCheckResult result = adapter.check(new MedicationContext(
                "Warfarin", "5mg", "daily", List.of("Aspirin 81mg"), List.of()));

        assertThat(result.interactions()).isNotEmpty();
        assertThat(result.interactions().get(0)).contains("Aspirin");
    }

    @Test
    void missingDosage_producesDoseWarning() {
        SafetyCheckResult result = adapter.check(new MedicationContext(
                "Ibuprofen", "", "PRN", List.of(), List.of()));

        assertThat(result.doseWarnings()).anyMatch(w -> w.contains("Dosage is missing"));
    }

    @Test
    void noConflicts_returnsEmptyResult() {
        SafetyCheckResult result = adapter.check(new MedicationContext(
                "Vitamin D", "1000IU", "daily", List.of("Calcium"), List.of("Penicillin")));

        assertThat(result.hasFindings()).isFalse();
        assertThat(result.interactions()).isEmpty();
        assertThat(result.allergyFlags()).isEmpty();
        assertThat(result.doseWarnings()).isEmpty();
    }

    @Test
    void sildenafilPlusNitrate_isFlagged() {
        SafetyCheckResult result = adapter.check(new MedicationContext(
                "Sildenafil", "50mg", "PRN", List.of("Nitroglycerin spray"), List.of()));

        assertThat(result.interactions()).isNotEmpty();
    }
}
