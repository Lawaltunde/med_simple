package com.medsimple.diagnostic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.diagnostic.dto.*;
import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.RequisitionStatus;
import com.medsimple.diagnostic.entity.RequisitionUrgency;
import com.medsimple.diagnostic.exception.ResultNotReleasedException;
import com.medsimple.diagnostic.service.DiagnosticService;
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
class DiagnosticControllerTest {

    @Mock DiagnosticService diagnosticService;
    @InjectMocks DiagnosticController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID id;
    private UUID carePlanId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        id = UUID.randomUUID();
        carePlanId = UUID.randomUUID();
    }

    @Test
    void createRequisition_returns201() throws Exception {
        RequisitionResponse resp = new RequisitionResponse(id, carePlanId, UUID.randomUUID(), UUID.randomUUID(),
                "CBC", List.of("58410-2"), "anemia workup", RequisitionUrgency.ROUTINE,
                RequisitionStatus.DRAFT, null, null, null, null, null, null, OffsetDateTime.now());
        when(diagnosticService.createRequisition(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/requisitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRequisitionRequest(
                                carePlanId, "CBC", List.of("58410-2"), "anemia workup", RequisitionUrgency.ROUTINE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testType").value("CBC"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void recordResult_returns201() throws Exception {
        PhysicianResultResponse resp = new PhysicianResultResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Hb 9.1", null, "summary", null, false, null, OffsetDateTime.now());
        when(diagnosticService.recordResult(eq(id), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/requisitions/{id}/result", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordResultRequest("Hb 9.1", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.releasedToPatient").value(false));
    }

    @Test
    void releaseResult_returns200() throws Exception {
        PhysicianResultResponse resp = new PhysicianResultResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Hb 9.1", null, "summary", "interp", true, OffsetDateTime.now(), OffsetDateTime.now());
        when(diagnosticService.releaseResult(eq(id), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/results/{id}/release", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releasedToPatient").value(true));
    }

    // The PHIPA gate at the HTTP boundary.
    @Test
    void patientResult_unreleased_returns403() throws Exception {
        when(diagnosticService.getResultForPatient(id)).thenThrow(new ResultNotReleasedException(id));

        mockMvc.perform(get("/api/v1/patient-results/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_RELEASED"));
    }

    @Test
    void patientResult_released_returns200WithSafeFields() throws Exception {
        PatientResultResponse resp = new PatientResultResponse(id, UUID.randomUUID(), "CBC",
                "Your blood count is slightly low.", "Mild anemia.", OffsetDateTime.now());
        when(diagnosticService.getResultForPatient(id)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/patient-results/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testType").value("CBC"))
                .andExpect(jsonPath("$.physicianInterpretation").value("Mild anemia."));
    }

    @Test
    void createRequisition_missingTestType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carePlanId\":\"" + carePlanId + "\",\"urgency\":\"ROUTINE\"}"))
                .andExpect(status().isBadRequest());
    }
}
