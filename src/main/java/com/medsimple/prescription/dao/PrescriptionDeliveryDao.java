package com.medsimple.prescription.dao;

import com.medsimple.prescription.entity.Prescription;

// One implementation per delivery channel. The active one is chosen at runtime,
// per clinic, by PrescriptionDeliveryResolver based on clinic_capabilities.
public interface PrescriptionDeliveryDao {

    DeliveryResult deliver(Prescription prescription);
}
