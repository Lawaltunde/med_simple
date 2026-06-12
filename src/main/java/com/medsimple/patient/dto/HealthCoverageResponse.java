package com.medsimple.patient.dto;

import com.medsimple.patient.entity.CoverageType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// Raw card number is never included — PHI stays encrypted at rest.
public record HealthCoverageResponse(
        UUID id,
        CoverageType coverageType,
        String providerName,
        String province,
        LocalDate expiryDate,
        boolean primary,
        boolean active,
        OffsetDateTime createdAt
) {}
