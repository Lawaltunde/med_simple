package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.ClinicType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClinicRequest(
        @NotBlank String name,
        @NotNull ClinicType type,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String postalCode,
        String phone,
        String fax,
        String email
) {}
