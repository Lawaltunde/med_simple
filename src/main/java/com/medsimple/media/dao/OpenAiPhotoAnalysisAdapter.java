package com.medsimple.media.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;

/**
 * GPT-4o vision analysis of a patient-submitted photo via Spring AI ChatClient.
 * The image URL is passed as media to the model. PHI: the image stays within the
 * platform's storage; only its URL is handed to the model provider.
 * Active when ai.media.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.media.adapter", havingValue = "openai")
public class OpenAiPhotoAnalysisAdapter implements PhotoAnalysisDao {

    private static final String PROMPT = """
            You are assisting a primary care clinician. Describe this patient-submitted medical
            image factually: what body area is shown and the visible clinical findings
            (e.g. redness, swelling, rash pattern, wound, discoloration). Do not diagnose or
            advise treatment. Two or three sentences.
            """;

    private final ChatClient chatClient;

    public OpenAiPhotoAnalysisAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String analyze(String mediaUrl) {
        try {
            UrlResource image = new UrlResource(URI.create(mediaUrl));
            return chatClient.prompt()
                    .user(u -> u.text(PROMPT).media(MimeTypeUtils.IMAGE_JPEG, image))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("OpenAI photo analysis failed for {}; falling back to clinician review note", mediaUrl, e);
            return "[Photo received. Automatic analysis failed; a clinician will review the image.]";
        }
    }
}
