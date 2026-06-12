package com.medsimple.prescription.dao;

public interface MedicationSafetyDao {

    SafetyCheckResult check(MedicationContext context);
}
