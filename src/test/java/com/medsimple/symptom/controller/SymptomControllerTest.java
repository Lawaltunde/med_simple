package com.medsimple.symptom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.symptom.dto.*;
import com.medsimple.symptom.entity.*;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.service.SymptomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SymptomControllerTest {

    @Mock SymptomService symptomService;
    @InjectMocks SymptomController symptomController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID patientId;
    private UUID episodeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(symptomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        patientId = UUID.randomUUID();
        episodeId = UUID.randomUUID();
    }

    private EpisodeResponse sampleEpisode() {
        return new EpisodeResponse(episodeId, patientId, null, null,
                EpisodeStatus.SUBMITTED, null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(), List.of());
    }

    @Test
    void startEpisode_returns201() throws Exception {
        when(symptomService.startEpisode(eq(patientId), any(), any())).thenReturn(sampleEpisode());

        mockMvc.perform(post("/api/v1/patients/{patientId}/episodes", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }

    @Test
    void getEpisode_found_returns200() throws Exception {
        when(symptomService.getEpisode(episodeId)).thenReturn(sampleEpisode());

        mockMvc.perform(get("/api/v1/episodes/{id}", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(episodeId.toString()));
    }

    @Test
    void getEpisode_notFound_returns404() throws Exception {
        when(symptomService.getEpisode(episodeId)).thenThrow(new EpisodeNotFoundException(episodeId));

        mockMvc.perform(get("/api/v1/episodes/{id}", episodeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void addTextSubmission_returns201() throws Exception {
        AddSubmissionRequest request = new AddSubmissionRequest(InputType.TEXT, "Chest pain", null);
        SubmissionResponse response = new SubmissionResponse(UUID.randomUUID(), InputType.TEXT,
                "Chest pain", null, null, null, ProcessingStatus.COMPLETED, OffsetDateTime.now());

        when(symptomService.addSubmission(eq(episodeId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/episodes/{id}/submissions", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inputType").value("TEXT"))
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"));
    }

    @Test
    void addSubmission_missingInputType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/episodes/{id}/submissions", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPatientEpisodes_returns200() throws Exception {
        when(symptomService.getPatientEpisodes(patientId)).thenReturn(List.of(sampleEpisode()));

        mockMvc.perform(get("/api/v1/patients/{patientId}/episodes", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(patientId.toString()));
    }
}
