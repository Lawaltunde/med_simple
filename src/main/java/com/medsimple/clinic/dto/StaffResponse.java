package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.StaffRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        StaffRole role,
        String licenseNumber,
        String specialization,
        boolean active,
        OffsetDateTime createdAt
) {}
