package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import com.medsimple.symptom.entity.MediaRelevance;
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

    private static PhotoAnalysisResult result(MediaRelevance relevance, String description) {
        return new PhotoAnalysisResult(relevance, description);
    }

    @Test
    void returnsAnalysis_onFirstSuccess() {
        OpenAiPhotoAnalysisAdapter adapter = newAdapter();
        when(callSpec.entity(PhotoAnalysisResult.class))
                .thenReturn(result(MediaRelevance.SYMPTOM_PRESENT, "Left forearm with localized redness."));

        PhotoAnalysisResult r = adapter.analyze(URL);
        assertThat(r.relevance()).isEqualTo(MediaRelevance.SYMPTOM_PRESENT);
        assertThat(r.description()).isEqualTo("Left forearm with localized redness.");
        verify(callSpec, times(1)).entity(PhotoAnalysisResult.class);
    }

    @Test
    void recoversTransientFailure_andReturnsRealAnalysis() {
        OpenAiPhotoAnalysisAdapter adapter = newAdapter();
        when(callSpec.entity(PhotoAnalysisResult.class))
                .thenThrow(new RuntimeException("429 Too Many Requests"))
                .thenReturn(result(MediaRelevance.SYMPTOM_PRESENT, "Rash on the cheek."));

        assertThat(adapter.analyze(URL).description()).isEqualTo("Rash on the cheek.");
        verify(callSpec, times(2)).entity(PhotoAnalysisResult.class);
    }

    @Test
    void fallsBackToUnsureNote_whenEveryAttemptFails() {
        OpenAiPhotoAnalysisAdapter adapter = newAdapter();
        when(callSpec.entity(PhotoAnalysisResult.class)).thenThrow(new RuntimeException("persistent failure"));

        PhotoAnalysisResult r = adapter.analyze(URL);
        assertThat(r.relevance()).isEqualTo(MediaRelevance.UNSURE); // never silently dropped
        assertThat(r.description()).contains("Photo").contains("clinician will review");
        verify(callSpec, times(3)).entity(PhotoAnalysisResult.class);
    }
}
