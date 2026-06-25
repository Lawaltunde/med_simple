package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.ByteArrayResource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiVoiceTranscriptionAdapterTest {

    private final OpenAiAudioTranscriptionModel model = mock(OpenAiAudioTranscriptionModel.class);
    private final MediaStorageDao storage = mock(MediaStorageDao.class);
    private final OpenAiVoiceTranscriptionAdapter adapter =
            new OpenAiVoiceTranscriptionAdapter(model, storage);

    private static final String URL = "/api/v1/media/" + UUID.randomUUID();

    private void storageResolves() {
        when(storage.resolve(URL)).thenReturn(
                new MediaContent(new ByteArrayResource("audio".getBytes()), "audio/webm", UUID.randomUUID()));
    }

    private static AudioTranscriptionResponse response(String text) {
        return new AudioTranscriptionResponse(new AudioTranscription(text));
    }

    @Test
    void returnsTranscription_onFirstSuccess() {
        storageResolves();
        when(model.call(any(AudioTranscriptionPrompt.class))).thenReturn(response("sore throat for two days"));

        assertThat(adapter.transcribe(URL)).isEqualTo("sore throat for two days");
        verify(model, times(1)).call(any(AudioTranscriptionPrompt.class));
    }

    @Test
    void recoversTransientFailure_andReturnsRealTranscription() {
        storageResolves();
        when(model.call(any(AudioTranscriptionPrompt.class)))
                .thenThrow(new RuntimeException("503 Service Unavailable"))
                .thenReturn(response("headache and fever"));

        // The exact regression: a single transient blip must NOT surface as a failure note.
        assertThat(adapter.transcribe(URL)).isEqualTo("headache and fever");
        verify(model, times(2)).call(any(AudioTranscriptionPrompt.class));
    }

    @Test
    void fallsBackToClinicianNote_whenEveryAttemptFails() {
        storageResolves();
        when(model.call(any(AudioTranscriptionPrompt.class)))
                .thenThrow(new RuntimeException("persistent failure"));

        String result = adapter.transcribe(URL);
        assertThat(result).contains("Voice note").contains("clinician will review");
        verify(model, times(3)).call(any(AudioTranscriptionPrompt.class));
    }
}
