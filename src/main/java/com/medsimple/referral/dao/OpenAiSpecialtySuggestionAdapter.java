package com.medsimple.referral.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring AI ChatClient adapter for specialty suggestion via OpenAI.
 * PHI is never included — only the clinical summary text reaches the model.
 * Active when ai.referral.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.referral.adapter", havingValue = "openai")
public class OpenAiSpecialtySuggestionAdapter implements SpecialtySuggestionDao {

    private static final String SYSTEM_PROMPT = """
            You are a referral triage assistant for a Canadian primary care clinic.
            Given a clinical summary, suggest the single most appropriate medical specialty
            for referral. Use standard Canadian specialty names (e.g. Cardiology, Dermatology,
            Gastroenterology, Neurology, Orthopedics, Respirology, Endocrinology, Psychiatry,
            Ophthalmology, ENT, Internal Medicine, General Surgery, Urology, Nephrology).
            Provide the specialty and a one-sentence rationale.
            """;

    private final ChatClient chatClient;

    public OpenAiSpecialtySuggestionAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public SpecialtySuggestion suggest(String clinicalSummary) {
        try {
            return chatClient.prompt()
                    .user("Clinical summary:\n" + clinicalSummary)
                    .call()
                    .entity(SpecialtySuggestion.class);
        } catch (Exception e) {
            log.error("OpenAI specialty suggestion failed; defaulting to Internal Medicine", e);
            return new SpecialtySuggestion("Internal Medicine",
                    "AI suggestion unavailable — physician to confirm the appropriate specialty.");
        }
    }
}
