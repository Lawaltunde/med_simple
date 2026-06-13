package com.medsimple.diagnostic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Full physician view — includes raw result data and release status.
public record PhysicianResultResponse(
        UUID id,
        UUID requisitionId,
        UUID patientId,
        UUID orderingPhysicianId,
        String rawResultText,
        String rawResultData,
        String aiSummary,
        String physicianInterpretation,
        boolean releasedToPatient,
        OffsetDateTime releasedToPatientAt,
        OffsetDateTime receivedAt
) {}
