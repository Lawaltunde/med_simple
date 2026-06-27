package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Voice-note transcription via valsea.ai's OpenAI-compatible REST endpoint
 * (POST {base-url}/audio/transcriptions, multipart file + model, response {"text": ...}).
 * Active when ai.voice.adapter=valsea.
 *
 * PHI: the mediaUrl is an auth-gated in-platform path (/api/v1/media/{id}), not externally
 * fetchable. The adapter resolves it to the stored bytes via {@link MediaStorageDao} and POSTs
 * ONLY the audio to Valsea — no patient id, no URL, no submission context. Mirrors the Whisper
 * adapter's retry + clinician-review fallback so a transient blip never becomes a permanent
 * "transcription failed" note.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.voice.adapter", havingValue = "valsea")
public class ValseaVoiceTranscriptionAdapter implements VoiceTranscriptionDao {

    // Same transient-failure handling as the Whisper adapter: 3 attempts, 250ms→500ms backoff.
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 250L;

    private final RestClient restClient;
    private final MediaStorageDao mediaStorage;
    private final String apiKey;
    private final String model;
    private final String language;

    public ValseaVoiceTranscriptionAdapter(RestClient.Builder restClientBuilder,
                                           MediaStorageDao mediaStorage,
                                           @Value("${valsea.api.base-url}") String baseUrl,
                                           @Value("${valsea.api.key}") String apiKey,
                                           @Value("${valsea.api.model}") String model,
                                           @Value("${valsea.api.language}") String language) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.mediaStorage = mediaStorage;
        this.apiKey = apiKey;
        this.model = model;
        this.language = language;
    }

    @Override
    public String transcribe(String mediaUrl) {
        try {
            return MediaModelRetry.withRetry("Valsea voice transcription", MAX_ATTEMPTS, BASE_BACKOFF_MILLIS,
                    () -> {
                        MediaContent media = mediaStorage.resolve(mediaUrl);
                        ValseaTranscriptionResponse response = restClient.post()
                                .uri("/audio/transcriptions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                                .body(multipartBody(media))
                                .retrieve()
                                .body(ValseaTranscriptionResponse.class);
                        if (response == null || response.text() == null || response.text().isBlank()) {
                            throw new IllegalStateException("Valsea returned an empty transcription");
                        }
                        return response.text();
                    });
        } catch (Exception e) {
            // Every attempt failed. Log the failure, not the media or any identifier.
            log.error("Valsea voice transcription failed after retries; falling back to clinician review note", e);
            return "[Voice note received. Automatic transcription failed; a clinician will review the recording.]";
        }
    }

    private MultiValueMap<String, Object> multipartBody(MediaContent media) {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(media.contentType()));
        fileHeaders.setContentDispositionFormData("file", filename(media.contentType()));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(media.resource(), fileHeaders));
        body.add("model", model);
        body.add("language", language);
        return body;
    }

    // OpenAI-compatible transcription APIs infer the audio format from the filename extension, so
    // derive one from the stored content type (audio/webm -> voice-note.webm). Falls back to .wav.
    private static String filename(String contentType) {
        String ext = "wav";
        if (contentType != null && contentType.contains("/")) {
            String subtype = contentType.substring(contentType.indexOf('/') + 1).trim();
            int semicolon = subtype.indexOf(';');
            if (semicolon >= 0) subtype = subtype.substring(0, semicolon).trim();
            if (subtype.equals("mpeg")) ext = "mp3";
            else if (!subtype.isBlank()) ext = subtype;
        }
        return "voice-note." + ext;
    }

    record ValseaTranscriptionResponse(String text) {}
}
