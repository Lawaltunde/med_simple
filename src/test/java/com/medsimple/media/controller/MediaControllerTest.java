package com.medsimple.media.controller;

import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.media.exception.MediaNotFoundException;
import com.medsimple.media.exception.UnsupportedMediaTypeException;
import com.medsimple.media.service.MediaStorageService;
import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.StoredMedia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock MediaStorageService mediaStorageService;
    @InjectMocks MediaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── upload ──────────────────────────────────────────────────────────────────

    @Test
    void upload_returns201WithMediaUrl() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaStorageService.upload(any(), eq("image/jpeg"), any()))
                .thenReturn(new StoredMedia(id, "/api/v1/media/" + id, "image/jpeg", UUID.randomUUID()));

        MockMultipartFile file = new MockMultipartFile(
                "file", "rash.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/media").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaUrl").value("/api/v1/media/" + id))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    void upload_unsupportedType_returns415() throws Exception {
        when(mediaStorageService.upload(any(), eq("application/pdf"), any()))
                .thenThrow(new UnsupportedMediaTypeException("application/pdf"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/media").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    // ── get (auth gate) ──────────────────────────────────────────────────────────

    @Test
    void get_returnsBytesWithContentTypeAndPrivateCache() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaStorageService.download(eq(id), any()))
                .thenReturn(new MediaContent(new ByteArrayResource(new byte[]{4, 5, 6}), "image/png", UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(content().bytes(new byte[]{4, 5, 6}));
    }

    @Test
    void get_othersMedia_deniedReturns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaStorageService.download(eq(id), any()))
                .thenThrow(new AccessDeniedException("You can only access your own records"));

        mockMvc.perform(get("/api/v1/media/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaStorageService.download(eq(id), any()))
                .thenThrow(new MediaNotFoundException(id));

        mockMvc.perform(get("/api/v1/media/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
