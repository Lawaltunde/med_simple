package com.medsimple.referral.dao;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keyword-based specialty suggestion — deterministic, no external calls.
 * Default adapter; active when ai.referral.adapter is not set to openai.
 * Maps common presentations to a specialty; defaults to Internal Medicine when unsure
 * (a safe generalist landing spot the physician can override).
 */
@Component
public class FallbackSpecialtySuggestionAdapter implements SpecialtySuggestionDao {

    // Ordered: first matching specialty wins. Keys are lowercased keywords.
    private static final Map<String, List<String>> SPECIALTY_KEYWORDS = new LinkedHashMap<>();
    static {
        SPECIALTY_KEYWORDS.put("Cardiology", List.of("chest pain", "heart", "palpitation", "arrhythmia", "hypertension", "cardiac"));
        SPECIALTY_KEYWORDS.put("Dermatology", List.of("skin", "rash", "mole", "lesion", "acne", "eczema", "psoriasis"));
        SPECIALTY_KEYWORDS.put("Gastroenterology", List.of("stomach", "abdominal", "bowel", "liver", "nausea", "reflux", "diarrhea", "constipation"));
        SPECIALTY_KEYWORDS.put("Neurology", List.of("headache", "migraine", "seizure", "numbness", "tingling", "dizziness", "tremor"));
        SPECIALTY_KEYWORDS.put("Orthopedics", List.of("joint", "fracture", "knee", "shoulder", "back pain", "bone", "sprain"));
        SPECIALTY_KEYWORDS.put("Respirology", List.of("breath", "asthma", "cough", "lung", "wheeze", "copd"));
        SPECIALTY_KEYWORDS.put("Endocrinology", List.of("diabetes", "thyroid", "hormone", "insulin"));
        SPECIALTY_KEYWORDS.put("Psychiatry", List.of("depression", "anxiety", "mood", "mental health", "panic"));
        SPECIALTY_KEYWORDS.put("Ophthalmology", List.of("eye", "vision", "blurred", "glaucoma"));
        SPECIALTY_KEYWORDS.put("ENT", List.of("ear", "throat", "sinus", "hearing", "nasal"));
    }

    private static final String DEFAULT_SPECIALTY = "Internal Medicine";

    @Override
    public SpecialtySuggestion suggest(String clinicalSummary) {
        String text = clinicalSummary == null ? "" : clinicalSummary.toLowerCase();
        for (Map.Entry<String, List<String>> entry : SPECIALTY_KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(text::contains)) {
                return new SpecialtySuggestion(entry.getKey(),
                        "Matched presentation keywords for " + entry.getKey() + ".");
            }
        }
        return new SpecialtySuggestion(DEFAULT_SPECIALTY,
                "No specific specialty pattern matched; routing to a generalist for assessment.");
    }
}
