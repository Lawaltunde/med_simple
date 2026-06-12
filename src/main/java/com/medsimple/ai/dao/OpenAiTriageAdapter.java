package com.medsimple.ai.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.symptom.entity.UrgencyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls OpenAI chat completions API for AI triage.
 * PHI is never included in the prompt — only symptom text reaches the model.
 * Active when ai.triage.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.triage.adapter", havingValue = "openai")
public class OpenAiTriageAdapter implements AiTriageDao {

    private static final String API_URL = "https://api.openai.com";
    private static final String MODEL = "gpt-4o";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;

    public OpenAiTriageAdapter(@Value("${openai.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public TriageResult analyze(EpisodeContext context) {
        String prompt = buildPrompt(context);

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            String response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("OpenAI triage failed for episode {}", context.episodeId(), e);
            return new TriageResult(UrgencyLevel.SEE_SOON,
                    "AI analysis unavailable — please review manually.");
        }
    }

    private TriageResult parseResponse(String raw) throws Exception {
        JsonNode root = MAPPER.readTree(raw);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        JsonNode result = MAPPER.readTree(content);
        UrgencyLevel urgency = UrgencyLevel.valueOf(result.get("urgencyLevel").asText());
        String summary = result.get("summary").asText();
        return new TriageResult(urgency, summary);
    }

    private String systemPrompt() {
        return """
                You are a medical triage assistant for a Canadian primary care clinic.
                Analyze patient-reported symptoms and return a JSON object with urgency and summary.
                Urgency levels: ROUTINE (scheduled appointment), SEE_SOON (24-48 hours),
                URGENT (same day), EMERGENCY (call 911 immediately).
                Always respond with valid JSON only: {"urgencyLevel": "...", "summary": "..."}
                """;
    }

    private String buildPrompt(EpisodeContext context) {
        String symptoms = String.join("\n- ", context.symptoms());
        return "Patient-reported symptoms:\n- " + symptoms;
    }
}
