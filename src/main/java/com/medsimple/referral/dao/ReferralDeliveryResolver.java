package com.medsimple.referral.dao;

import com.medsimple.clinic.entity.CapabilityType;
import com.medsimple.clinic.repository.ClinicCapabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chooses the referral delivery channel at runtime, per clinic — the
 * clinic_capabilities-driven adapter selection the architecture mandates:
 * Ocean eReferral when the clinic has it enabled, PDF fallback otherwise.
 */
@Component
@RequiredArgsConstructor
public class ReferralDeliveryResolver {

    private final ClinicCapabilityRepository capabilityRepository;
    private final OceanEReferralAdapter oceanAdapter;
    private final PdfFallbackReferralAdapter pdfFallbackAdapter;

    public ReferralDeliveryDao resolve(UUID clinicId) {
        boolean oceanEnabled = capabilityRepository
                .findByClinicIdAndCapability(clinicId, CapabilityType.OCEAN_EREFERRAL)
                .map(c -> c.isEnabled())
                .orElse(false);
        return oceanEnabled ? oceanAdapter : pdfFallbackAdapter;
    }
}
