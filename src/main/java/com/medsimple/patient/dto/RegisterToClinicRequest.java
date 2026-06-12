package com.medsimple.patient.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterToClinicRequest(
        @NotNull UUID clinicId
) {}
