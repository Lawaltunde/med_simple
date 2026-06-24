package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.StaffRole;
import com.medsimple.clinic.entity.VerificationStatus;

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
        String college,
        String registrationNumber,
        String licenseProvince,
        String billingNumber,
        VerificationStatus verificationStatus,
        String verificationSource,
        OffsetDateTime verifiedAt,
        boolean active,
        OffsetDateTime createdAt
) {}
