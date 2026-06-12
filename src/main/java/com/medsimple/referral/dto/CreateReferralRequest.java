package com.medsimple.referral.dto;

import com.medsimple.referral.entity.ReferralUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReferralRequest(
        @NotNull UUID carePlanId,
        @NotBlank String clinicalSummary,
        @NotNull ReferralUrgency urgency
) {}
