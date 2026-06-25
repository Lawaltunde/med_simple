package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiPhotoAnalysisAdapterTest {

    private final ChatClient.Builder builder = mock(ChatClient.Builder.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
    private final MediaStorageDao storage = mock(MediaStorageDao.class);

    private static final String URL = "/api/v1/media/" + UUID.randomUUID();

    private OpenAiPhotoAnalysisAdapter newAdapter() {
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(storage.resolve(URL)).thenReturn(
                new MediaContent(new ByteArrayResource("img".getBytes()), "image/png", UUID.randomUUID()));
        return new OpenAiPhotoAnalysisAdapter(builder, storage);
    }

    @Test
    void returnsAnalysis_onFirstSuccess() {
        OpenAiPhotoAnalysisAdapter adapter = newAdapter();
        when(callSpec.content()).thenReturn("Left forearm with localized redness.");

        assertThat(adapter.analyze(URL)).isEqualTo("Left forearm with localized redness.");
        verify(callSpec, times(1)).content();
    }

    @Test
    void recoversTransientFailure_andReturnsRealAnalysis() {
        OpenAiPhotoAnalysisAdapter adapter = newAdapter();
        when(callSpec.content())
                .thenThrow(new RuntimeException("429 Too Many Requests"))
                .thenReturn("Rash on the cheek.");

        assertThat(adapter.analyze(URL)).isEqualTo("Rash on the cheek.");
        verify(callSpec, times(2)).content();
    }

    @Test
    void fallsBackToClinicianNote_whenEveryAttemptFails() {
        OpenAiPhotoAnalysisAdapter adapter = newAdapter();
        when(callSpec.content()).thenThrow(new RuntimeException("persistent failure"));

        assertThat(adapter.analyze(URL)).contains("Photo").contains("clinician will review");
        verify(callSpec, times(3)).content();
    }
}
