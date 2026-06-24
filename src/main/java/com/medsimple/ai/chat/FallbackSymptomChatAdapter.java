package com.medsimple.ai.chat;

import com.medsimple.symptom.entity.UrgencyLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic symptom-chat fallback (no model calls) — the default unless ai.chat.adapter=openai.
 * Keeps the feature safe and demoable offline: it never diagnoses, always recommends a clinician,
 * and still escalates on obvious emergency keywords in the latest patient message.
 */
@Component
public class FallbackSymptomChatAdapter implements SymptomChatDao {

    private static final List<String> EMERGENCY_TERMS = List.of(
            "chest pain", "can't breathe", "cannot breathe", "trouble breathing", "stroke",
            "severe bleeding", "unconscious", "suicid", "anaphylax", "overdose");

    @Override
    public SymptomChatReply reply(List<ChatTurn> conversation) {
        String lastUser = conversation.stream()
                .filter(t -> !"assistant".equalsIgnoreCase(t.role()))
                .map(ChatTurn::content)
                .reduce((a, b) -> b)
                .orElse("")
                .toLowerCase(Locale.ROOT);

        boolean emergency = EMERGENCY_TERMS.stream().anyMatch(lastUser::contains);
        if (emergency) {
            return new SymptomChatReply(
                    "Your symptoms may be a medical emergency. Please call 911 or go to the nearest "
                            + "emergency department now.",
                    UrgencyLevel.EMERGENCY, List.of("Possible emergency symptom reported"), true,
                    "Patient reported possible emergency symptoms via chat.");
        }
        return new SymptomChatReply(
                "Thanks for sharing. I can't diagnose, but it's best to have a clinician assess "
                        + "this. You can send your symptoms to a doctor for review, and if anything "
                        + "gets worse or feels urgent, seek care right away.",
                UrgencyLevel.SEE_SOON, List.of(), true,
                "Patient described symptoms via chat; clinician review recommended.");
    }

    @Override
    public String adapterName() {
        return "fallback";
    }
}
