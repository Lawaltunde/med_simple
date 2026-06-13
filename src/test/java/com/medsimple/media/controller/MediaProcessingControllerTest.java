package com.medsimple.media.controller;

import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.media.dto.ProcessedSubmissionResponse;
import com.medsimple.media.exception.SubmissionNotFoundException;
import com.medsimple.media.service.MediaProcessingService;
import com.medsimple.symptom.entity.InputType;
import com.medsimple.symptom.entity.ProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaProcessingControllerTest {

    @Mock MediaProcessingService mediaProcessingService;
    @InjectMocks MediaProcessingController controller;

    private MockMvc mockMvc;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        submissionId = UUID.randomUUID();
    }

    @Test
    void process_returns200WithResult() throws Exception {
        ProcessedSubmissionResponse response = new ProcessedSubmissionResponse(
                submissionId, InputType.PHOTO, "storage://p.jpg", null,
                "Erythematous rash on the forearm", ProcessingStatus.COMPLETED);
        when(mediaProcessingService.process(submissionId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/submissions/{id}/process", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.aiAnalysisResult").value("Erythematous rash on the forearm"));
    }

    @Test
    void process_notFound_returns404() throws Exception {
        when(mediaProcessingService.process(submissionId))
                .thenThrow(new SubmissionNotFoundException(submissionId));

        mockMvc.perform(post("/api/v1/submissions/{id}/process", submissionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
