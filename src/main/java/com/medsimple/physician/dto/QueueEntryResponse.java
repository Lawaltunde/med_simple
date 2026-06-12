package com.medsimple.physician.dto;

import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.UrgencyLevel;

import java.time.OffsetDateTime;
import java.util.UUID;

// One row in the physician queue. effectiveUrgency drives ordering:
// the physician-confirmed level if present, otherwise the AI-assessed level.
public record QueueEntryResponse(
        UUID episodeId,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        UrgencyLevel effectiveUrgency,
        UrgencyLevel aiUrgencyLevel,
        UrgencyLevel confirmedUrgencyLevel,
        EpisodeStatus status,
        String aiSummary,
        UUID assignedPhysicianId,
        OffsetDateTime createdAt
) {}
