package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import com.medsimple.symptom.entity.MediaRelevance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * GPT-4o vision analysis of a patient-submitted photo via Spring AI ChatClient.
 * Active when ai.photo.adapter=openai.
 *
 * PHI: the mediaUrl is an auth-gated in-platform path (/api/v1/media/{id}), not externally
 * fetchable. The adapter resolves it to the stored bytes via {@link MediaStorageDao} and
 * hands ONLY the image bytes to the model — no patient id, no URL, no submission context.
 * The image content itself is the sole thing that leaves the platform, by design.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.photo.adapter", havingValue = "openai")
public class OpenAiPhotoAnalysisAdapter implements PhotoAnalysisDao {

    private static final String PROMPT = """
            You are assisting a primary care clinician. A patient uploaded this image. Do NOT assume
            it shows a medical concern — it might be unrelated (a pet, a screenshot, a selfie with
            nothing visibly wrong).

            First decide whether the image shows a visible MEDICAL symptom (rash, wound, swelling,
            redness, discoloration, injury, or similar):
            - relevance = SYMPTOM_PRESENT if a visible symptom is clearly shown
            - relevance = NO_SYMPTOM if the image does not show a medical symptom
            - relevance = UNSURE if it is too blurry or ambiguous to tell

            Then write a factual two-or-three-sentence description:
            - If SYMPTOM_PRESENT: the body area shown and the visible findings. Do not diagnose or
              advise treatment.
            - If NO_SYMPTOM: state plainly that no visible medical symptom is present and, briefly,
              what the image appears to show. Do NOT invent clinical findings.
            - If UNSURE: say what is unclear.
            """;

    // GPT-4o vision fails intermittently on valid images (timeout / rate-limit / 5xx); an
    // immediate retry clears it. Three attempts with 250ms→500ms backoff before the fallback.
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 250L;

    private final ChatClient chatClient;
    private final MediaStorageDao mediaStorage;

    public OpenAiPhotoAnalysisAdapter(ChatClient.Builder chatClientBuilder, MediaStorageDao mediaStorage) {
        this.chatClient = chatClientBuilder.build();
        this.mediaStorage = mediaStorage;
    }

    @Override
    public PhotoAnalysisResult analyze(String mediaUrl) {
        try {
            return MediaModelRetry.withRetry("OpenAI photo analysis", MAX_ATTEMPTS, BASE_BACKOFF_MILLIS,
                    () -> {
                        MediaContent media = mediaStorage.resolve(mediaUrl);
                        MimeType mimeType = MimeTypeUtils.parseMimeType(media.contentType());
                        return chatClient.prompt()
                                .user(u -> u.text(PROMPT).media(mimeType, media.resource()))
                                .call()
                                .entity(PhotoAnalysisResult.class);
                    });
        } catch (Exception e) {
            // Every attempt failed. UNSURE (never silently dropped) so a clinician still reviews.
            // Log the failure, not the media or any identifier.
            log.error("OpenAI photo analysis failed after retries; falling back to clinician review note", e);
            return new PhotoAnalysisResult(MediaRelevance.UNSURE,
                    "[Photo received. Automatic analysis failed; a clinician will review the image.]");
        }
    }
}
