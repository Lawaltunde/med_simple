package com.medsimple.referral.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.referral.dto.ConfirmSpecialtyRequest;
import com.medsimple.referral.dto.CreateReferralRequest;
import com.medsimple.referral.dto.ReferralResponse;
import com.medsimple.referral.entity.ReferralDeliveryMethod;
import com.medsimple.referral.entity.ReferralStatus;
import com.medsimple.referral.entity.ReferralUrgency;
import com.medsimple.referral.exception.InvalidReferralStateException;
import com.medsimple.referral.exception.ReferralNotFoundException;
import com.medsimple.referral.service.ReferralService;
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
class ReferralControllerTest {

    @Mock ReferralService referralService;
    @InjectMocks ReferralController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID refId;
    private UUID carePlanId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        refId = UUID.randomUUID();
        carePlanId = UUID.randomUUID();
    }

    private ReferralResponse response(ReferralStatus status, ReferralDeliveryMethod method,
                                      String externalId, String docUrl) {
        return new ReferralResponse(refId, carePlanId, UUID.randomUUID(), UUID.randomUUID(),
                "Cardiology", null, "chest pain", ReferralUrgency.ROUTINE,
                status, method, externalId, docUrl, null, OffsetDateTime.now());
    }

    @Test
    void create_returns201() throws Exception {
        when(referralService.createReferral(any(), any()))
                .thenReturn(response(ReferralStatus.DRAFT, null, null, null));

        mockMvc.perform(post("/api/v1/referrals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReferralRequest(
                                carePlanId, "Recurrent chest pain", ReferralUrgency.URGENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.aiSuggestedSpecialty").value("Cardiology"));
    }

    @Test
    void create_missingSummary_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/referrals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carePlanId\":\"" + carePlanId + "\",\"urgency\":\"ROUTINE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmSpecialty_returns200() throws Exception {
        ReferralResponse resp = new ReferralResponse(refId, carePlanId, UUID.randomUUID(), UUID.randomUUID(),
                "Cardiology", "Respirology", "chest pain", ReferralUrgency.ROUTINE,
                ReferralStatus.DRAFT, null, null, null, null, OffsetDateTime.now());
        when(referralService.confirmSpecialty(eq(refId), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/referrals/{id}/confirm-specialty", refId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmSpecialtyRequest("Respirology"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedSpecialty").value("Respirology"));
    }

    @Test
    void submit_returns200_pdfPath() throws Exception {
        when(referralService.submitReferral(eq(refId), any()))
                .thenReturn(response(ReferralStatus.SUBMITTED, ReferralDeliveryMethod.PDF_PRINT, null, "storage://ref.pdf"));

        mockMvc.perform(post("/api/v1/referrals/{id}/submit", refId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.documentUrl").value("storage://ref.pdf"));
    }

    @Test
    void submit_invalidState_returns409() throws Exception {
        when(referralService.submitReferral(eq(refId), any()))
                .thenThrow(new InvalidReferralStateException("Only a DRAFT referral can be submitted"));

        mockMvc.perform(post("/api/v1/referrals/{id}/submit", refId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(referralService.getReferral(refId)).thenThrow(new ReferralNotFoundException(refId));

        mockMvc.perform(get("/api/v1/referrals/{id}", refId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
