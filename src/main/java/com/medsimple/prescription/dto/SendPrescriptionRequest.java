package com.medsimple.prescription.dto;

import java.util.UUID;

// Optional target pharmacy for electronic delivery.
public record SendPrescriptionRequest(
        UUID pharmacyId
) {}
