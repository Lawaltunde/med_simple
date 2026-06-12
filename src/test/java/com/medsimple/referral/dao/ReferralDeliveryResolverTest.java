package com.medsimple.referral.dao;

import com.medsimple.clinic.entity.CapabilityType;
import com.medsimple.clinic.entity.ClinicCapability;
import com.medsimple.clinic.repository.ClinicCapabilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralDeliveryResolverTest {

    @Mock ClinicCapabilityRepository capabilityRepository;
    @Mock OceanEReferralAdapter oceanAdapter;
    @Mock PdfFallbackReferralAdapter pdfFallbackAdapter;

    @InjectMocks ReferralDeliveryResolver resolver;

    @Test
    void oceanEnabled_resolvesToOceanAdapter() {
        UUID clinicId = UUID.randomUUID();
        ClinicCapability cap = ClinicCapability.builder()
                .capability(CapabilityType.OCEAN_EREFERRAL).enabled(true).build();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.OCEAN_EREFERRAL))
                .thenReturn(Optional.of(cap));

        assertThat(resolver.resolve(clinicId)).isSameAs(oceanAdapter);
    }

    @Test
    void oceanDisabled_resolvesToPdfFallback() {
        UUID clinicId = UUID.randomUUID();
        ClinicCapability cap = ClinicCapability.builder()
                .capability(CapabilityType.OCEAN_EREFERRAL).enabled(false).build();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.OCEAN_EREFERRAL))
                .thenReturn(Optional.of(cap));

        assertThat(resolver.resolve(clinicId)).isSameAs(pdfFallbackAdapter);
    }

    @Test
    void noCapabilityRecord_resolvesToPdfFallback() {
        UUID clinicId = UUID.randomUUID();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.OCEAN_EREFERRAL))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolve(clinicId)).isSameAs(pdfFallbackAdapter);
    }
}
