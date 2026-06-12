package com.medsimple.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.patient.dto.*;
import com.medsimple.patient.entity.CoverageType;
import com.medsimple.patient.exception.EmailAlreadyInUseException;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.service.PatientService;
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
class PatientControllerTest {

    @Mock PatientService patientService;
    @InjectMocks PatientController patientController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        patientId = UUID.randomUUID();
    }

    // ── POST /api/v1/patients ────────────────────────────────────────────────

    @Test
    void createPatient_returns201() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest(
                "jane@example.com", "password123", "Jane", "Doe", null, null);
        PatientResponse response = new PatientResponse(
                patientId, "jane@example.com", "Jane", "Doe", null,
                true, false, OffsetDateTime.now(),
                new PatientResponse.Profile(null, null, List.of(), List.of()));

        when(patientService.createPatient(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void createPatient_missingEmail_returns400() throws Exception {
        String body = """
                {"password":"password123","firstName":"Jane","lastName":"Doe"}
                """;
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPatient_duplicateEmail_returns409() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest(
                "jane@example.com", "password123", "Jane", "Doe", null, null);

        when(patientService.createPatient(any(), any()))
                .thenThrow(new EmailAlreadyInUseException("jane@example.com"));

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // ── GET /api/v1/patients/{id} ────────────────────────────────────────────

    @Test
    void getPatient_found_returns200() throws Exception {
        PatientResponse response = new PatientResponse(
                patientId, "jane@example.com", "Jane", "Doe", null,
                true, false, OffsetDateTime.now(),
                new PatientResponse.Profile(null, null, List.of(), List.of()));

        when(patientService.getPatient(patientId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()));
    }

    @Test
    void getPatient_notFound_returns404() throws Exception {
        when(patientService.getPatient(patientId)).thenThrow(new PatientNotFoundException(patientId));

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ── POST /api/v1/patients/{id}/coverage ──────────────────────────────────

    @Test
    void addCoverage_returns201() throws Exception {
        AddHealthCoverageRequest request = new AddHealthCoverageRequest(
                CoverageType.PROVINCIAL, "Ontario OHIP", "AB123456", "ON", null, true);
        HealthCoverageResponse response = new HealthCoverageResponse(
                UUID.randomUUID(), CoverageType.PROVINCIAL, "Ontario OHIP",
                "ON", null, true, true, OffsetDateTime.now());

        when(patientService.addHealthCoverage(eq(patientId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients/{id}/coverage", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverageType").value("PROVINCIAL"))
                .andExpect(jsonPath("$.providerName").value("Ontario OHIP"));
    }

    // ── POST /api/v1/patients/{id}/registrations ─────────────────────────────

    @Test
    void registerToClinic_returns201() throws Exception {
        UUID clinicId = UUID.randomUUID();
        RegisterToClinicRequest request = new RegisterToClinicRequest(clinicId);
        ClinicRegistrationResponse response = new ClinicRegistrationResponse(
                UUID.randomUUID(), clinicId, "Maple Clinic", null, true, OffsetDateTime.now());

        when(patientService.registerToClinic(eq(patientId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients/{id}/registrations", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clinicName").value("Maple Clinic"));
    }

    // ── DELETE /api/v1/patients/{id}/coverage/{coverageId} ───────────────────

    @Test
    void deactivateCoverage_returns204() throws Exception {
        UUID coverageId = UUID.randomUUID();
        doNothing().when(patientService).deactivateHealthCoverage(patientId, coverageId);

        mockMvc.perform(delete("/api/v1/patients/{id}/coverage/{coverageId}", patientId, coverageId))
                .andExpect(status().isNoContent());
    }
}
