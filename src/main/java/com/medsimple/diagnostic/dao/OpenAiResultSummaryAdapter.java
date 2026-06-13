package com.medsimple.diagnostic.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring AI ChatClient adapter that produces a plain-language summary of a diagnostic result.
 * PHI is never included — only the test type and raw result text reach the model.
 * The summary is an aid; the physician's interpretation is authoritative and gates release.
 * Active when ai.result.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.result.adapter", havingValue = "openai")
public class OpenAiResultSummaryAdapter implements ResultSummaryDao {

    private static final String SYSTEM_PROMPT = """
            You translate diagnostic test results into plain language a patient can understand.
            Explain what the test measures and what the values indicate in everyday terms.
            Be factual and calm. Do NOT give a diagnosis, prognosis, or treatment advice —
            state that the physician will review and discuss the results.
            Keep it to two or three short sentences.
            """;

    private final ChatClient chatClient;

    public OpenAiResultSummaryAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public String summarize(String testType, String rawResultText) {
        try {
            return chatClient.prompt()
                    .user("Test type: " + testType + "\nResult:\n" + rawResultText)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("OpenAI result summary failed for {}; using neutral placeholder", testType, e);
            return "Your " + testType + " results are available and will be reviewed by your physician.";
        }
    }
}
