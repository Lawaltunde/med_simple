package com.medsimple.diagnostic.dto;

import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.RequisitionStatus;
import com.medsimple.diagnostic.entity.RequisitionUrgency;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RequisitionResponse(
        UUID id,
        UUID carePlanId,
        UUID patientId,
        UUID orderingPhysicianId,
        String testType,
        List<String> testCodes,
        String clinicalIndication,
        RequisitionUrgency urgency,
        RequisitionStatus status,
        DiagnosticDeliveryMethod deliveryMethod,
        UUID diagnosticCentreId,
        String documentUrl,
        OffsetDateTime appointmentAt,
        OffsetDateTime sentAt,
        OffsetDateTime bookedAt,
        OffsetDateTime createdAt
) {}
