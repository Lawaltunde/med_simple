package com.medsimple.ai.dao;

import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.symptom.entity.UrgencyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring AI ChatClient adapter for OpenAI.
 * PHI is never included in the prompt — only symptom text reaches the model.
 * Structured output (.entity) maps the model response straight to TriageResult.
 * Active when ai.triage.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.triage.adapter", havingValue = "openai")
public class OpenAiTriageAdapter implements AiTriageDao {

    private static final String SYSTEM_PROMPT = """
            You are a medical triage assistant for a Canadian primary care clinic.
            Analyze the patient-reported symptoms and determine the urgency level.

            URGENCY LEVELS:
            - ROUTINE: Can wait for a scheduled appointment (days to weeks)
            - SEE_SOON: Should be seen within 24-48 hours
            - URGENT: Should be seen today or go to urgent care
            - EMERGENCY: Call 911 or go to emergency immediately

            Provide the urgency level and a brief clinical summary in one or two sentences.
            """;

    private final ChatClient chatClient;

    public OpenAiTriageAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public TriageResult analyze(EpisodeContext context) {
        String userMessage = "Patient-reported symptoms:\n- " + String.join("\n- ", context.symptoms());
        try {
            return chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(TriageResult.class);
        } catch (Exception e) {
            log.error("OpenAI triage failed for episode {}", context.episodeId(), e);
            return new TriageResult(UrgencyLevel.SEE_SOON,
                    "AI analysis unavailable — please review manually.");
        }
    }
}
