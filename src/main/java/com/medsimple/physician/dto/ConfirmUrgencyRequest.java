package com.medsimple.physician.dto;

import com.medsimple.symptom.entity.UrgencyLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Physician confirms or overrides the AI-assessed urgency.
// overrideReason is required only when the confirmed level differs from the AI level.
public record ConfirmUrgencyRequest(
        @NotNull UUID physicianId,
        @NotNull UrgencyLevel confirmedUrgencyLevel,
        String overrideReason
) {}
