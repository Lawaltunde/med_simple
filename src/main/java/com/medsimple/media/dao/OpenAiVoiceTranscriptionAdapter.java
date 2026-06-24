package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Whisper transcription of a patient voice note via Spring AI's OpenAiAudioTranscriptionModel.
 * Active when ai.media.adapter=openai.
 *
 * PHI: the mediaUrl is an auth-gated in-platform path (/api/v1/media/{id}), not externally
 * fetchable. The adapter resolves it to the stored bytes via {@link MediaStorageDao} and
 * sends ONLY the audio to Whisper — no patient id, no URL, no submission context.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.media.adapter", havingValue = "openai")
public class OpenAiVoiceTranscriptionAdapter implements VoiceTranscriptionDao {

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final MediaStorageDao mediaStorage;

    public OpenAiVoiceTranscriptionAdapter(OpenAiAudioTranscriptionModel transcriptionModel,
                                           MediaStorageDao mediaStorage) {
        this.transcriptionModel = transcriptionModel;
        this.mediaStorage = mediaStorage;
    }

    @Override
    public String transcribe(String mediaUrl) {
        try {
            MediaContent media = mediaStorage.resolve(mediaUrl);
            return transcriptionModel.call(new AudioTranscriptionPrompt(media.resource()))
                    .getResult().getOutput();
        } catch (Exception e) {
            // Log the failure, not the media or any identifier.
            log.error("OpenAI voice transcription failed; falling back to clinician review note", e);
            return "[Voice note received. Automatic transcription failed; a clinician will review the recording.]";
        }
    }
}
