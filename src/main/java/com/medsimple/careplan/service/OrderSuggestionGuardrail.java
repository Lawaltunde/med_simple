package com.medsimple.careplan.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic post-filter on the AI's suggested ORDERS (referral specialty + diagnostic tests).
 * This is the latent->deterministic meta-loop (CLAUDE.md): the model proposes, this code-level screen
 * guarantees the model cannot cross the line. It does NOT trust the model to resist prompt injection
 * — it screens the OUTPUT regardless of what the prompt did.
 *
 * The suggestion layer proposes routine, non-invasive investigations and a recognised specialty ONLY.
 * Anything invasive, procedural, or controlled-substance-related is dropped here, never persisted, and
 * never one-click-prefilled into a physician's create form. The physician remains the decision-maker:
 * these are advisory prefills they review and submit through the existing referral / diagnostic flow.
 */
@Component
public class OrderSuggestionGuardrail {

    // At most this many tests survive — a long list is a sign the model is over-ordering.
    public static final int MAX_TESTS = 5;

    // Invasive / procedural / controlled terms a SUGGESTED test must never contain. A suggested
    // diagnostic is a routine, non-invasive investigation (labs, basic imaging) only. Anything that
    // implies a procedure, an invasive sample, or a controlled-substance workflow is dropped.
    private static final List<String> BANNED_TEST_TERMS = List.of(
            "biopsy", "surgery", "surgical", "operation", "resection", "excision", "incision",
            "endoscopy", "endoscop", "colonoscopy", "gastroscopy", "bronchoscopy", "cystoscopy",
            "laparoscopy", "arthroscopy", "catheter", "cannula", "intubat", "lumbar puncture",
            "spinal tap", "paracentesis", "thoracentesis", "aspiration", "angiogram", "angiography",
            "angioplasty", "cardiac cath", "stent", "drain insertion", "needle", "injection",
            "contrast injection", "anaesthe", "anesthe", "sedation", "narcotic", "opioid", "opiate",
            "controlled substance", "ablation", "dilation and curettage", "amniocentesis");

    // Specialties the AI is allowed to suggest a referral to. An unrecognised value is dropped rather
    // than passed through — the model does not get to invent a destination. Stored lowercase; the
    // canonical (title-cased) form is returned so the prefill reads cleanly.
    private static final List<String> KNOWN_SPECIALTIES = List.of(
            "Cardiology", "Dermatology", "Endocrinology", "Gastroenterology", "Hematology",
            "Nephrology", "Neurology", "Oncology", "Ophthalmology", "Orthopedics", "Otolaryngology",
            "Psychiatry", "Pulmonology", "Rheumatology", "Urology", "Gynecology", "Obstetrics",
            "General Surgery", "Vascular Surgery", "Plastic Surgery", "Neurosurgery", "Allergy",
            "Immunology", "Infectious Disease", "Pain Management", "Physiatry", "Geriatrics",
            "Pediatrics", "Nutrition", "Physiotherapy", "Respirology", "Internal Medicine");

    /**
     * Screen and cap the AI's suggested tests. Returns a sanitized list: banned/invasive terms
     * dropped, blanks dropped, de-duplicated case-insensitively, trimmed, capped at {@link #MAX_TESTS}.
     * Returns null (not empty) when nothing survives, so persistence stores NULL.
     */
    public List<String> sanitizeTests(List<String> tests) {
        if (tests == null || tests.isEmpty()) {
            return null;
        }
        List<String> kept = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String raw : tests) {
            if (raw == null) continue;
            String test = raw.trim();
            if (test.isEmpty()) continue;
            String lower = test.toLowerCase(Locale.ROOT);
            if (isBannedTest(lower)) continue;
            if (!seen.add(lower)) continue; // de-dup
            kept.add(test);
            if (kept.size() >= MAX_TESTS) break;
        }
        return kept.isEmpty() ? null : kept;
    }

    /** True if a suggested test names an invasive / procedural / controlled item and must be dropped. */
    public boolean isBannedTest(String testLower) {
        if (testLower == null || testLower.isBlank()) {
            return true;
        }
        return BANNED_TEST_TERMS.stream().anyMatch(testLower::contains);
    }

    /**
     * Map the AI's suggested specialty to the canonical name from the known list, or null if it is
     * unrecognised. Matching is case-insensitive and trims surrounding noise; an unknown value is
     * dropped rather than passed through.
     */
    public String sanitizeSpecialty(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return null;
        }
        String normalized = specialty.trim().toLowerCase(Locale.ROOT);
        return KNOWN_SPECIALTIES.stream()
                .filter(known -> known.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
