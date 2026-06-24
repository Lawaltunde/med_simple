package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * GPT-4o vision analysis of a patient-submitted photo via Spring AI ChatClient.
 * Active when ai.media.adapter=openai.
 *
 * PHI: the mediaUrl is an auth-gated in-platform path (/api/v1/media/{id}), not externally
 * fetchable. The adapter resolves it to the stored bytes via {@link MediaStorageDao} and
 * hands ONLY the image bytes to the model — no patient id, no URL, no submission context.
 * The image content itself is the sole thing that leaves the platform, by design.
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
    private final MediaStorageDao mediaStorage;

    public OpenAiPhotoAnalysisAdapter(ChatClient.Builder chatClientBuilder, MediaStorageDao mediaStorage) {
        this.chatClient = chatClientBuilder.build();
        this.mediaStorage = mediaStorage;
    }

    @Override
    public String analyze(String mediaUrl) {
        try {
            MediaContent media = mediaStorage.resolve(mediaUrl);
            MimeType mimeType = MimeTypeUtils.parseMimeType(media.contentType());
            return chatClient.prompt()
                    .user(u -> u.text(PROMPT).media(mimeType, media.resource()))
                    .call()
                    .content();
        } catch (Exception e) {
            // Log the failure, not the media or any identifier.
            log.error("OpenAI photo analysis failed; falling back to clinician review note", e);
            return "[Photo received. Automatic analysis failed; a clinician will review the image.]";
        }
    }
}
