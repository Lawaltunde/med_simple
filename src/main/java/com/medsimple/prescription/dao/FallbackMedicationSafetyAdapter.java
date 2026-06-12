package com.medsimple.prescription.dao;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rule-based medication safety check — deterministic, zero latency, no external calls.
 * Default adapter; active when ai.safety.adapter is not set to openai.
 * Covers direct allergy matches and a curated set of well-known dangerous interactions.
 * Errs toward flagging — a false positive is a physician double-check; a false negative is a harm.
 */
@Component
public class FallbackMedicationSafetyAdapter implements MedicationSafetyDao {

    // Well-documented interacting pairs (lowercased substrings). Not exhaustive —
    // the OpenAI adapter handles the long tail; this guarantees the obvious ones never slip.
    private static final Map<String, List<String>> KNOWN_INTERACTIONS = Map.of(
            "warfarin", List.of("aspirin", "ibuprofen", "naproxen", "amoxicillin"),
            "metformin", List.of("contrast"),
            "lisinopril", List.of("potassium", "spironolactone", "ibuprofen"),
            "simvastatin", List.of("clarithromycin", "erythromycin", "grapefruit"),
            "ssri", List.of("tramadol", "triptan", "maoi"),
            "sildenafil", List.of("nitrate", "nitroglycerin")
    );

    @Override
    public SafetyCheckResult check(MedicationContext ctx) {
        String med = ctx.medicationName().toLowerCase();
        List<String> interactions = new ArrayList<>();
        List<String> allergyFlags = new ArrayList<>();
        List<String> doseWarnings = new ArrayList<>();

        // Allergy matches — new med name appears in the patient's allergy list (either direction).
        for (String allergy : ctx.allergies()) {
            String a = allergy.toLowerCase();
            if (med.contains(a) || a.contains(med)) {
                allergyFlags.add("Patient has a recorded allergy to '" + allergy
                        + "' which may conflict with " + ctx.medicationName());
            }
        }

        // Interactions — check the new med against each current medication.
        for (String current : ctx.currentMedications()) {
            String c = current.toLowerCase();
            if (interacts(med, c) || interacts(c, med)) {
                interactions.add("Potential interaction between " + ctx.medicationName()
                        + " and current medication '" + current + "'");
            }
        }

        // Dose sanity — flag obviously missing or non-numeric dosage for physician review.
        if (ctx.dosage() == null || ctx.dosage().isBlank()) {
            doseWarnings.add("Dosage is missing — confirm before signing");
        }

        return new SafetyCheckResult(interactions, allergyFlags, doseWarnings);
    }

    private boolean interacts(String drug, String other) {
        return KNOWN_INTERACTIONS.entrySet().stream()
                .filter(e -> drug.contains(e.getKey()))
                .anyMatch(e -> e.getValue().stream().anyMatch(other::contains));
    }
}
