package com.medsimple.shared.dto;

import jakarta.validation.constraints.NotBlank;

public record AllergyDto(
        @NotBlank String name,
        String severity
) {}
