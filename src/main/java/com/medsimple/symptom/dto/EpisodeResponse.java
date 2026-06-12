package com.medsimple.symptom.dto;

import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.UrgencyLevel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EpisodeResponse(
        UUID id,
        UUID patientId,
        UUID assignedClinicId,
        UUID assignedPhysicianId,
        EpisodeStatus status,
        UrgencyLevel aiUrgencyLevel,
        String aiSummary,
        OffsetDateTime aiAnalyzedAt,
        UrgencyLevel confirmedUrgencyLevel,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<SubmissionResponse> submissions
) {}
