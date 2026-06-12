package com.medsimple.ai.dao;

import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.symptom.entity.UrgencyLevel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Keyword-based triage adapter — deterministic, zero latency, no external calls.
 * Used when ai.triage.adapter is not set to claude-cli (default), and in all tests.
 * Covers common emergency and urgent presentations; errs toward higher urgency when uncertain.
 */
@Component
public class FallbackAiTriageAdapter implements AiTriageDao {

    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "chest pain", "can't breathe", "cannot breathe", "difficulty breathing",
            "shortness of breath", "stroke", "heart attack", "unconscious", "unresponsive",
            "severe bleeding", "bleeding heavily", "seizure", "anaphylaxis", "allergic reaction",
            "overdose", "suicidal", "loss of consciousness"
    );

    private static final List<String> URGENT_KEYWORDS = List.of(
            "high fever", "fever", "severe pain", "vomiting", "infection",
            "fracture", "broken", "head injury", "concussion", "abdominal pain",
            "back pain", "urinary", "eye pain", "ear pain", "swelling",
            "dizzy", "dizziness", "fainting", "palpitations"
    );

    private static final List<String> SEE_SOON_KEYWORDS = List.of(
            "cough", "sore throat", "runny nose", "cold", "mild pain",
            "rash", "skin", "fatigue", "tired", "headache", "nausea",
            "indigestion", "bloating", "constipation", "diarrhea"
    );

    @Override
    public TriageResult analyze(EpisodeContext context) {
        String combined = String.join(" ", context.symptoms()).toLowerCase();

        if (containsAny(combined, EMERGENCY_KEYWORDS)) {
            return new TriageResult(UrgencyLevel.EMERGENCY,
                    "Symptoms suggest a potential emergency. Immediate medical attention required.");
        }
        if (containsAny(combined, URGENT_KEYWORDS)) {
            return new TriageResult(UrgencyLevel.URGENT,
                    "Symptoms require same-day assessment. Please visit urgent care or your clinic today.");
        }
        if (containsAny(combined, SEE_SOON_KEYWORDS)) {
            return new TriageResult(UrgencyLevel.SEE_SOON,
                    "Symptoms should be assessed within 24-48 hours. Please book an appointment.");
        }
        return new TriageResult(UrgencyLevel.ROUTINE,
                "Symptoms appear non-urgent. Please book a routine appointment with your physician.");
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
