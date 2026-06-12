package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.CapabilityType;

import java.util.UUID;

public record CapabilityResponse(
        UUID id,
        CapabilityType capability,
        boolean enabled
) {}
