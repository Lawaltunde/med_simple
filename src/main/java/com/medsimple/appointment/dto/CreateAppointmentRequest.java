package com.medsimple.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

// A physician schedules a visit/follow-up for an episode. Patient + clinic are derived from it.
public record CreateAppointmentRequest(
        @NotNull UUID episodeId,
        @NotNull UUID physicianId,
        @NotNull OffsetDateTime scheduledAt,
        Integer durationMinutes,
        String reason
) {}
