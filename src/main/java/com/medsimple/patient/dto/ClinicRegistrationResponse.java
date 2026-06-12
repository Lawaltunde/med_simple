package com.medsimple.patient.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClinicRegistrationResponse(
        UUID id,
        UUID clinicId,
        String clinicName,
        UUID assignedPhysicianId,
        boolean active,
        OffsetDateTime registeredAt
) {}
