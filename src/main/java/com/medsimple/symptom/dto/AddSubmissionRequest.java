package com.medsimple.symptom.dto;

import com.medsimple.symptom.entity.InputType;
import jakarta.validation.constraints.NotNull;

public record AddSubmissionRequest(
        @NotNull InputType inputType,
        String textContent,
        String mediaUrl
) {}
