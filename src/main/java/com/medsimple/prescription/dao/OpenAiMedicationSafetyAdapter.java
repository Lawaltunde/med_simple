package com.medsimple.prescription.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI ChatClient adapter for medication safety review via OpenAI.
 * PHI is never included — only medication, dose, current meds, and allergies reach the model.
 * Active when ai.safety.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.safety.adapter", havingValue = "openai")
public class OpenAiMedicationSafetyAdapter implements MedicationSafetyDao {

    private static final String SYSTEM_PROMPT = """
            You are a clinical pharmacist assistant for a Canadian primary care clinic.
            Review a proposed prescription against the patient's current medications and allergies.
            Identify, as three separate lists:
            - interactions: drug-drug interactions between the new medication and current medications
            - allergyFlags: conflicts with the patient's recorded allergies
            - doseWarnings: dosing concerns (too high/low, frequency, missing information)
            Each entry is a short clinical sentence. Use empty lists when there are no concerns.
            Do not invent concerns that are not clinically supported.
            """;

    private final ChatClient chatClient;

    public OpenAiMedicationSafetyAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public SafetyCheckResult check(MedicationContext ctx) {
        String userMessage = """
                Proposed medication: %s
                Dosage: %s
                Frequency: %s
                Current medications: %s
                Known allergies: %s
                """.formatted(
                ctx.medicationName(), ctx.dosage(), ctx.frequency(),
                ctx.currentMedications().isEmpty() ? "none" : String.join(", ", ctx.currentMedications()),
                ctx.allergies().isEmpty() ? "none" : String.join(", ", ctx.allergies()));

        try {
            return chatClient.prompt().user(userMessage).call().entity(SafetyCheckResult.class);
        } catch (Exception e) {
            log.error("OpenAI medication safety check failed for {}; flagging for manual review",
                    ctx.medicationName(), e);
            return new SafetyCheckResult(
                    List.of(),
                    List.of(),
                    List.of("AI safety check unavailable — physician must review manually before signing"));
        }
    }
}
