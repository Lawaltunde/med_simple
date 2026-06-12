package com.medsimple.careplan.dto;

import com.medsimple.careplan.entity.CarePlanStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CarePlanResponse(
        UUID id,
        UUID episodeId,
        UUID patientId,
        UUID physicianId,
        UUID clinicId,
        CarePlanStatus status,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<FollowUpResponse> followUps
) {}
