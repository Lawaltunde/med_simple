package com.medsimple.prescription.dao;

import com.medsimple.prescription.entity.DeliveryMethod;
import com.medsimple.prescription.entity.PrescriptionStatus;

// Outcome of dispatching a signed prescription. documentUrl is set only for the PDF path.
public record DeliveryResult(
        PrescriptionStatus status,
        DeliveryMethod deliveryMethod,
        String documentUrl
) {}
