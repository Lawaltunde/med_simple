package com.medsimple.careplan.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

// initiatedBy is the physician (or null for a system/AI-generated prompt — set system=true).
public record AddFollowUpRequest(
        UUID initiatedBy,
        boolean system,
        @NotBlank String message
) {}
