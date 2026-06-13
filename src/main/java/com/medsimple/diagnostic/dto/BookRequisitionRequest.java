package com.medsimple.diagnostic.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookRequisitionRequest(
        @NotNull UUID diagnosticCentreId,
        OffsetDateTime appointmentAt
) {}
