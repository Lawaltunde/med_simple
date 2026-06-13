package com.medsimple.diagnostic.dto;

import jakarta.validation.constraints.NotBlank;

public record AddInterpretationRequest(
        @NotBlank String physicianInterpretation
) {}
