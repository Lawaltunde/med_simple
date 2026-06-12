package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.CapabilityType;
import jakarta.validation.constraints.NotNull;

public record SetCapabilityRequest(
        @NotNull CapabilityType capability,
        boolean enabled,
        String config
) {}
