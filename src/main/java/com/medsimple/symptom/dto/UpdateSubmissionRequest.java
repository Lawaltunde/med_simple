package com.medsimple.symptom.dto;

import com.medsimple.symptom.entity.ProcessingStatus;
import jakarta.validation.constraints.NotNull;

// Called by the AI service when analysis of a voice/photo submission completes.
public record UpdateSubmissionRequest(
        @NotNull ProcessingStatus processingStatus,
        String transcription,
        String aiAnalysisResult
) {}
