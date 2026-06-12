package com.medsimple.symptom.dto;

import com.medsimple.symptom.entity.InputType;
import com.medsimple.symptom.entity.ProcessingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        InputType inputType,
        String textContent,
        String mediaUrl,
        String transcription,
        String aiAnalysisResult,
        ProcessingStatus processingStatus,
        OffsetDateTime submittedAt
) {}
