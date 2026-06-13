package com.medsimple.diagnostic.dao;

import com.medsimple.diagnostic.entity.DiagnosticRequisition;

// One implementation per delivery channel; selected at runtime per clinic
// by RequisitionDeliveryResolver based on clinic_capabilities.
public interface RequisitionDeliveryDao {

    RequisitionDeliveryResult deliver(DiagnosticRequisition requisition);
}
