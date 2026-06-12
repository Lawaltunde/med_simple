package com.medsimple.physician.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.physician.dto.AssignPhysicianRequest;
import com.medsimple.physician.dto.ConfirmUrgencyRequest;
import com.medsimple.physician.dto.QueueEntryResponse;
import com.medsimple.physician.service.PhysicianQueueService;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.UrgencyLevel;
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
class PhysicianQueueControllerTest {

    @Mock PhysicianQueueService queueService;
    @InjectMocks PhysicianQueueController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID clinicId;
    private UUID episodeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        clinicId = UUID.randomUUID();
        episodeId = UUID.randomUUID();
    }

    private QueueEntryResponse entry(UrgencyLevel urgency, String first) {
        return new QueueEntryResponse(UUID.randomUUID(), UUID.randomUUID(), first, "Doe",
                urgency, urgency, null, EpisodeStatus.AI_ANALYZED, "summary", null, OffsetDateTime.now());
    }

    @Test
    void getQueue_returns200Ordered() throws Exception {
        when(queueService.getQueue(clinicId)).thenReturn(List.of(
                entry(UrgencyLevel.EMERGENCY, "First"),
                entry(UrgencyLevel.ROUTINE, "Last")));

        mockMvc.perform(get("/api/v1/clinics/{clinicId}/queue", clinicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].effectiveUrgency").value("EMERGENCY"))
                .andExpect(jsonPath("$[1].effectiveUrgency").value("ROUTINE"));
    }

    @Test
    void getQueue_clinicNotFound_returns404() throws Exception {
        when(queueService.getQueue(clinicId)).thenThrow(new ClinicNotFoundException(clinicId));

        mockMvc.perform(get("/api/v1/clinics/{clinicId}/queue", clinicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void assignPhysician_returns200() throws Exception {
        UUID physicianId = UUID.randomUUID();
        QueueEntryResponse response = new QueueEntryResponse(episodeId, UUID.randomUUID(),
                "Jane", "Doe", UrgencyLevel.URGENT, UrgencyLevel.URGENT, null,
                EpisodeStatus.ASSIGNED, "summary", physicianId, OffsetDateTime.now());

        when(queueService.assignPhysician(eq(episodeId), eq(physicianId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/episodes/{id}/assign", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignPhysicianRequest(physicianId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedPhysicianId").value(physicianId.toString()));
    }

    @Test
    void confirmUrgency_returns200() throws Exception {
        UUID physicianId = UUID.randomUUID();
        QueueEntryResponse response = new QueueEntryResponse(episodeId, UUID.randomUUID(),
                "Jane", "Doe", UrgencyLevel.EMERGENCY, UrgencyLevel.ROUTINE, UrgencyLevel.EMERGENCY,
                EpisodeStatus.ASSIGNED, "summary", null, OffsetDateTime.now());

        when(queueService.confirmUrgency(eq(episodeId), any(), any())).thenReturn(response);

        ConfirmUrgencyRequest request = new ConfirmUrgencyRequest(
                physicianId, UrgencyLevel.EMERGENCY, "Red flags missed");

        mockMvc.perform(post("/api/v1/episodes/{id}/confirm-urgency", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedUrgencyLevel").value("EMERGENCY"));
    }

    @Test
    void assignPhysician_missingPhysicianId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/episodes/{id}/assign", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
