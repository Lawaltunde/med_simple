package com.medsimple.referral.dto;

import jakarta.validation.constraints.NotBlank;

// Physician confirms or overrides the AI-suggested specialty before submission.
public record ConfirmSpecialtyRequest(
        @NotBlank String confirmedSpecialty
) {}
