package com.medsimple.prescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.prescription.dto.CreatePrescriptionRequest;
import com.medsimple.prescription.dto.PrescriptionResponse;
import com.medsimple.prescription.entity.DeliveryMethod;
import com.medsimple.prescription.entity.PrescriptionStatus;
import com.medsimple.prescription.exception.InvalidPrescriptionStateException;
import com.medsimple.prescription.exception.PrescriptionNotFoundException;
import com.medsimple.prescription.service.PrescriptionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTest {

    @Mock PrescriptionService prescriptionService;
    @InjectMocks PrescriptionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID rxId;
    private UUID carePlanId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        rxId = UUID.randomUUID();
        carePlanId = UUID.randomUUID();
    }

    private PrescriptionResponse response(PrescriptionStatus status, DeliveryMethod method, String docUrl) {
        return new PrescriptionResponse(rxId, carePlanId, UUID.randomUUID(), UUID.randomUUID(),
                "Amoxicillin", null, "500mg", "TID", "7 days", 0, "Take with food",
                "{\"interactions\":[],\"allergyFlags\":[],\"doseWarnings\":[]}",
                status, method, null, docUrl, null, null, null, OffsetDateTime.now());
    }

    @Test
    void create_returns201() throws Exception {
        when(prescriptionService.createPrescription(any(), any()))
                .thenReturn(response(PrescriptionStatus.DRAFT, null, null));

        mockMvc.perform(post("/api/v1/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePrescriptionRequest(
                                carePlanId, "Amoxicillin", null, "500mg", "TID", "7 days", 0, "Take with food"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.medicationName").value("Amoxicillin"));
    }

    @Test
    void create_missingMedicationName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carePlanId\":\"" + carePlanId + "\",\"dosage\":\"500mg\",\"frequency\":\"TID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sign_returns200() throws Exception {
        when(prescriptionService.signPrescription(eq(rxId), any()))
                .thenReturn(response(PrescriptionStatus.SIGNED, null, null));

        mockMvc.perform(post("/api/v1/prescriptions/{id}/sign", rxId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));
    }

    @Test
    void send_returns200_pdfPath() throws Exception {
        when(prescriptionService.sendPrescription(eq(rxId), any(), any()))
                .thenReturn(response(PrescriptionStatus.SENT_PDF, DeliveryMethod.PDF_PRINT, "storage://rx.pdf"));

        mockMvc.perform(post("/api/v1/prescriptions/{id}/send", rxId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT_PDF"))
                .andExpect(jsonPath("$.documentUrl").value("storage://rx.pdf"));
    }

    @Test
    void send_invalidState_returns409() throws Exception {
        when(prescriptionService.sendPrescription(eq(rxId), any(), any()))
                .thenThrow(new InvalidPrescriptionStateException("Only a SIGNED prescription can be sent"));

        mockMvc.perform(post("/api/v1/prescriptions/{id}/send", rxId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(prescriptionService.getPrescription(rxId)).thenThrow(new PrescriptionNotFoundException(rxId));

        mockMvc.perform(get("/api/v1/prescriptions/{id}", rxId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
