package com.medsimple.shared.dto;

import jakarta.validation.constraints.NotBlank;

public record MedicationDto(
        @NotBlank String name,
        String dose,
        String frequency
) {}
