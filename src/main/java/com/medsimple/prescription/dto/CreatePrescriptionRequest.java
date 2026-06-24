package com.medsimple.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

// refills is nullable so an omitted field is valid (defaults to 0 in the service).
public record CreatePrescriptionRequest(
        @NotNull UUID episodeId,
        @NotBlank String medicationName,
        String medicationCode,
        @NotBlank String dosage,
        @NotBlank String frequency,
        String duration,
        @PositiveOrZero Integer refills,
        String instructions
) {}
