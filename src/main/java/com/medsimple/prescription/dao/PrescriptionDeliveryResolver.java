package com.medsimple.prescription.dao;

import com.medsimple.clinic.entity.CapabilityType;
import com.medsimple.clinic.repository.ClinicCapabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chooses the delivery channel for a prescription at runtime, per clinic.
 * This is the clinic_capabilities-driven adapter selection the architecture mandates:
 * PrescribeIT when the clinic has it enabled, PDF fallback otherwise.
 */
@Component
@RequiredArgsConstructor
public class PrescriptionDeliveryResolver {

    private final ClinicCapabilityRepository capabilityRepository;
    private final PrescribeItDeliveryAdapter prescribeItAdapter;
    private final PdfFallbackDeliveryAdapter pdfFallbackAdapter;

    public PrescriptionDeliveryDao resolve(UUID clinicId) {
        boolean prescribeItEnabled = capabilityRepository
                .findByClinicIdAndCapability(clinicId, CapabilityType.PRESCRIBIT)
                .map(c -> c.isEnabled())
                .orElse(false);
        return prescribeItEnabled ? prescribeItAdapter : pdfFallbackAdapter;
    }
}
