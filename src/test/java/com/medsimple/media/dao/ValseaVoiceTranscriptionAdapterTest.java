package com.medsimple.media.dao;

import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ValseaVoiceTranscriptionAdapterTest {

    private static final String MEDIA_URL = "/api/v1/media/abc";
    private static final String ENDPOINT = "https://api.valsea.ai/v1/audio/transcriptions";

    private final MediaStorageDao mediaStorage = mock(MediaStorageDao.class);

    private MediaContent audio() {
        return new MediaContent(new ByteArrayResource("fake-audio-bytes".getBytes()), "audio/wav", UUID.randomUUID());
    }

    private ValseaVoiceTranscriptionAdapter adapterFor(RestClient.Builder builder) {
        when(mediaStorage.resolve(MEDIA_URL)).thenReturn(audio());
        return new ValseaVoiceTranscriptionAdapter(builder, mediaStorage,
                "https://api.valsea.ai/v1", "test-key", "valsea-transcribe", "english");
    }

    @Test
    void returnsTranscriptTextFromValseaResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("{\"text\":\"sore throat and fever for two days\"}",
                        MediaType.APPLICATION_JSON));

        String result = adapterFor(builder).transcribe(MEDIA_URL);

        assertThat(result).isEqualTo("sore throat and fever for two days");
        server.verify();
    }

    @Test
    void fallsBackToClinicianNoteWhenValseaFailsEveryAttempt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(times(3), requestTo(ENDPOINT)).andRespond(withServerError());

        String result = adapterFor(builder).transcribe(MEDIA_URL);

        assertThat(result).contains("a clinician will review the recording");
        server.verify();
    }
}
