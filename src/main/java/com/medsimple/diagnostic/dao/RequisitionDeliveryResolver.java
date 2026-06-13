package com.medsimple.diagnostic.dao;

import com.medsimple.clinic.entity.CapabilityType;
import com.medsimple.clinic.repository.ClinicCapabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chooses the requisition delivery channel at runtime, per clinic — the
 * clinic_capabilities-driven adapter selection: electronic lab submission when the
 * clinic has LAB_ELECTRONIC enabled, printable PDF requisition otherwise.
 */
@Component
@RequiredArgsConstructor
public class RequisitionDeliveryResolver {

    private final ClinicCapabilityRepository capabilityRepository;
    private final ElectronicLabAdapter electronicAdapter;
    private final PdfFallbackRequisitionAdapter pdfFallbackAdapter;

    public RequisitionDeliveryDao resolve(UUID clinicId) {
        boolean electronicEnabled = capabilityRepository
                .findByClinicIdAndCapability(clinicId, CapabilityType.LAB_ELECTRONIC)
                .map(c -> c.isEnabled())
                .orElse(false);
        return electronicEnabled ? electronicAdapter : pdfFallbackAdapter;
    }
}
