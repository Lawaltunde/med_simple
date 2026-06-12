package com.medsimple.prescription.dto;

import com.medsimple.prescription.entity.DeliveryMethod;
import com.medsimple.prescription.entity.PrescriptionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID carePlanId,
        UUID patientId,
        UUID prescribingPhysicianId,
        String medicationName,
        String medicationCode,
        String dosage,
        String frequency,
        String duration,
        int refills,
        String instructions,
        // Raw JSON of {interactions, allergyFlags, doseWarnings}
        String aiSafetyChecks,
        PrescriptionStatus status,
        DeliveryMethod deliveryMethod,
        UUID pharmacyId,
        String documentUrl,
        OffsetDateTime signedAt,
        OffsetDateTime sentAt,
        OffsetDateTime filledAt,
        OffsetDateTime createdAt
) {}
