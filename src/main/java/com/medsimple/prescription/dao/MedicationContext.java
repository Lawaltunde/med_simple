package com.medsimple.prescription.dao;

import java.util.List;

// PHI-stripped input to the medication safety check.
// Only the clinical facts needed for interaction/allergy/dose analysis — no patient identity.
public record MedicationContext(
        String medicationName,
        String dosage,
        String frequency,
        List<String> currentMedications,
        List<String> allergies
) {}
