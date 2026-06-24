package com.medsimple.ai.chat;

import com.medsimple.symptom.entity.UrgencyLevel;

import java.util.List;

/**
 * Structured reply from the symptom-chat model. The model returns BOTH the conversational message
 * and a machine-readable safety assessment, so escalation is deterministic on our side, not parsed
 * out of prose.
 */
public record SymptomChatReply(
        String reply,
        UrgencyLevel urgency,
        List<String> redFlags,
        boolean recommendSeeClinician,
        String symptomSummary
) {}
