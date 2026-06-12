package com.medsimple.patient.dto;

import com.medsimple.patient.entity.CoverageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddHealthCoverageRequest(
        @NotNull CoverageType coverageType,
        @NotBlank String providerName,
        @NotBlank String cardNumber,
        String province,
        LocalDate expiryDate,
        boolean primary
) {}
