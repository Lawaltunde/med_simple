package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.ClinicType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClinicResponse(
        UUID id,
        String name,
        ClinicType type,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String postalCode,
        String phone,
        String fax,
        String email,
        boolean active,
        OffsetDateTime createdAt
) {}
