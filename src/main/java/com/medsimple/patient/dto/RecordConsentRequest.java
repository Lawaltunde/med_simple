package com.medsimple.patient.dto;

import com.medsimple.patient.entity.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record RecordConsentRequest(
        @NotNull ConsentType consentType,
        @NotBlank String version,
        OffsetDateTime grantedAt,
        OffsetDateTime revokedAt
) {}
