package com.medsimple.careplan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RejectSuggestionRequest(
        @NotNull UUID physicianId,
        String reason
) {}
