package com.medsimple.careplan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCarePlanRequest(
        @NotNull UUID episodeId,
        @NotNull UUID physicianId,
        String notes
) {}
