package com.medsimple.media.dto;

import com.medsimple.symptom.entity.InputType;
import com.medsimple.symptom.entity.ProcessingStatus;

import java.util.UUID;

public record ProcessedSubmissionResponse(
        UUID id,
        InputType inputType,
        String mediaUrl,
        String transcription,
        String aiAnalysisResult,
        ProcessingStatus processingStatus
) {}
