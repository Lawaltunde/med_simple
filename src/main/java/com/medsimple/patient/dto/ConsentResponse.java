package com.medsimple.patient.dto;

import com.medsimple.patient.entity.ConsentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsentResponse(
        UUID id,
        ConsentType consentType,
        String version,
        OffsetDateTime grantedAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt
) {}
