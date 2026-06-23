package com.medsimple.appointment.dto;

import com.medsimple.appointment.entity.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID physicianId,
        UUID clinicId,
        UUID episodeId,
        UUID carePlanId,
        OffsetDateTime scheduledAt,
        int durationMinutes,
        String reason,
        AppointmentStatus status,
        String clinicName,
        String physicianName,
        String patientName,
        OffsetDateTime createdAt
) {}
