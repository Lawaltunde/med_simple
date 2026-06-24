package com.medsimple.clinic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.clinic.dto.*;
import com.medsimple.clinic.entity.ClinicType;
import com.medsimple.clinic.entity.StaffRole;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.clinic.service.ClinicService;
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
class ClinicControllerTest {

    @Mock ClinicService clinicService;
    @InjectMocks ClinicController clinicController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID clinicId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clinicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        clinicId = UUID.randomUUID();
    }

    @Test
    void createClinic_returns201() throws Exception {
        CreateClinicRequest request = new CreateClinicRequest(
                "Maple Clinic", ClinicType.FAMILY_PRACTICE,
                "123 Main St", null, "Toronto", "ON", "M5V 1A1",
                "416-555-0100", null, "maple@clinic.ca");
        ClinicResponse response = new ClinicResponse(clinicId, "Maple Clinic",
                ClinicType.FAMILY_PRACTICE, "123 Main St", null, "Toronto", "ON",
                "M5V 1A1", "416-555-0100", null, "maple@clinic.ca", true, OffsetDateTime.now());

        when(clinicService.createClinic(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clinics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maple Clinic"))
                .andExpect(jsonPath("$.type").value("FAMILY_PRACTICE"));
    }

    @Test
    void getClinic_found_returns200() throws Exception {
        ClinicResponse response = new ClinicResponse(clinicId, "Maple Clinic",
                ClinicType.FAMILY_PRACTICE, null, null, null, null,
                null, null, null, null, true, OffsetDateTime.now());

        when(clinicService.getClinic(clinicId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/clinics/{id}", clinicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clinicId.toString()));
    }

    @Test
    void getClinic_notFound_returns404() throws Exception {
        when(clinicService.getClinic(clinicId)).thenThrow(new ClinicNotFoundException(clinicId));

        mockMvc.perform(get("/api/v1/clinics/{id}", clinicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deactivateClinic_returns204() throws Exception {
        doNothing().when(clinicService).deactivateClinic(clinicId);

        mockMvc.perform(delete("/api/v1/clinics/{id}", clinicId))
                .andExpect(status().isNoContent());
    }

    @Test
    void addStaff_returns201() throws Exception {
        AddStaffRequest request = new AddStaffRequest(
                "dr@clinic.ca", "password123", "John", "Smith",
                null, StaffRole.PHYSICIAN, "ON-123", null,
                "CPSO", "67890", "ON", null);
        StaffResponse response = new StaffResponse(UUID.randomUUID(), UUID.randomUUID(),
                "John", "Smith", "dr@clinic.ca", StaffRole.PHYSICIAN,
                "ON-123", null, "CPSO", "67890", "ON", null,
                com.medsimple.clinic.entity.VerificationStatus.UNVERIFIED, null, null,
                true, OffsetDateTime.now());

        when(clinicService.addStaff(eq(clinicId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clinics/{id}/staff", clinicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("dr@clinic.ca"))
                .andExpect(jsonPath("$.role").value("PHYSICIAN"));
    }

    @Test
    void createClinic_missingName_returns400() throws Exception {
        String body = """
                {"type":"FAMILY_PRACTICE"}
                """;
        mockMvc.perform(post("/api/v1/clinics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
