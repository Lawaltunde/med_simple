package com.medsimple.careplan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.careplan.dto.*;
import com.medsimple.careplan.entity.CarePlanStatus;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.service.CarePlanService;
import com.medsimple.config.GlobalExceptionHandler;
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
class CarePlanControllerTest {

    @Mock CarePlanService carePlanService;
    @InjectMocks CarePlanController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID carePlanId;
    private UUID episodeId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        carePlanId = UUID.randomUUID();
        episodeId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CarePlanResponse sampleResponse() {
        return new CarePlanResponse(carePlanId, episodeId, UUID.randomUUID(), physicianId,
                UUID.randomUUID(), CarePlanStatus.ACTIVE, "Rest", OffsetDateTime.now(),
                OffsetDateTime.now(), List.of());
    }

    @Test
    void createCarePlan_returns201() throws Exception {
        when(carePlanService.createCarePlan(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/care-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCarePlanRequest(episodeId, physicianId, "Rest"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.episodeId").value(episodeId.toString()));
    }

    @Test
    void createCarePlan_missingEpisodeId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/care-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"physicianId\":\"" + physicianId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCarePlan_found_returns200() throws Exception {
        when(carePlanService.getCarePlan(carePlanId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/care-plans/{id}", carePlanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(carePlanId.toString()));
    }

    @Test
    void getCarePlan_notFound_returns404() throws Exception {
        when(carePlanService.getCarePlan(carePlanId)).thenThrow(new CarePlanNotFoundException(carePlanId));

        mockMvc.perform(get("/api/v1/care-plans/{id}", carePlanId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void addFollowUp_returns201() throws Exception {
        FollowUpResponse response = new FollowUpResponse(UUID.randomUUID(), physicianId, false,
                "How are you feeling?", null, null, null, OffsetDateTime.now());
        when(carePlanService.addFollowUp(eq(carePlanId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/care-plans/{id}/follow-ups", carePlanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddFollowUpRequest(physicianId, false, "How are you feeling?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("How are you feeling?"));
    }

    @Test
    void respondToFollowUp_returns200() throws Exception {
        UUID followUpId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();
        FollowUpResponse response = new FollowUpResponse(followUpId, physicianId, false,
                "How are you feeling?", "Much better", responderId, OffsetDateTime.now(), OffsetDateTime.now());
        when(carePlanService.respondToFollowUp(eq(carePlanId), eq(followUpId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/care-plans/{id}/follow-ups/{fid}/respond", carePlanId, followUpId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RespondFollowUpRequest(responderId, "Much better"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Much better"));
    }
}
