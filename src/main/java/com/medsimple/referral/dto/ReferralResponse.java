package com.medsimple.referral.dto;

import com.medsimple.referral.entity.ReferralDeliveryMethod;
import com.medsimple.referral.entity.ReferralStatus;
import com.medsimple.referral.entity.ReferralUrgency;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReferralResponse(
        UUID id,
        UUID episodeId,
        UUID carePlanId,
        UUID patientId,
        UUID referringPhysicianId,
        String aiSuggestedSpecialty,
        String confirmedSpecialty,
        String clinicalSummary,
        ReferralUrgency urgency,
        ReferralStatus status,
        ReferralDeliveryMethod deliveryMethod,
        String externalReferralId,
        String documentUrl,
        OffsetDateTime submittedAt,
        OffsetDateTime createdAt
) {}
