package com.medsimple.physician.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignPhysicianRequest(
        @NotNull UUID physicianId
) {}
