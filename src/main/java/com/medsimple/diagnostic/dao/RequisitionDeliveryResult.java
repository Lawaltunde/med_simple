package com.medsimple.diagnostic.dao;

import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.RequisitionStatus;

// Outcome of sending a requisition. documentUrl set only for the PDF path.
public record RequisitionDeliveryResult(
        RequisitionStatus status,
        DiagnosticDeliveryMethod deliveryMethod,
        String documentUrl
) {}
