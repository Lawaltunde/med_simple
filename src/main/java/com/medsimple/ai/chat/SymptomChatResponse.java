package com.medsimple.ai.chat;

import com.medsimple.symptom.entity.UrgencyLevel;

import java.util.List;

/**
 * What the patient UI receives. {@code emergency} drives the 911 banner; {@code symptomSummary}
 * pre-fills the "send to a doctor" hand-off; {@code disclaimer} is always shown.
 */
public record SymptomChatResponse(
        String reply,
        UrgencyLevel urgency,
        boolean emergency,
        List<String> redFlags,
        String symptomSummary,
        String disclaimer
) {}
