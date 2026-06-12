package com.medsimple.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RespondFollowUpRequest(
        @NotNull UUID respondedBy,
        @NotBlank String response
) {}
