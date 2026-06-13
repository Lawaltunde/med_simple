package com.medsimple.media.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Whisper transcription of a patient voice note via Spring AI's OpenAiAudioTranscriptionModel.
 * Fetches the audio from mediaUrl and returns the transcribed text.
 * Active when ai.media.adapter=openai.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.media.adapter", havingValue = "openai")
public class OpenAiVoiceTranscriptionAdapter implements VoiceTranscriptionDao {

    private final OpenAiAudioTranscriptionModel transcriptionModel;

    public OpenAiVoiceTranscriptionAdapter(OpenAiAudioTranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    @Override
    public String transcribe(String mediaUrl) {
        try {
            UrlResource audio = new UrlResource(URI.create(mediaUrl));
            return transcriptionModel.call(new AudioTranscriptionPrompt(audio)).getResult().getOutput();
        } catch (Exception e) {
            log.error("OpenAI voice transcription failed for {}; falling back to clinician review note", mediaUrl, e);
            return "[Voice note received. Automatic transcription failed; a clinician will review the recording.]";
        }
    }
}
