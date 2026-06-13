package com.medsimple.diagnostic.dao;

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
class RequisitionDeliveryResolverTest {

    @Mock ClinicCapabilityRepository capabilityRepository;
    @Mock ElectronicLabAdapter electronicAdapter;
    @Mock PdfFallbackRequisitionAdapter pdfFallbackAdapter;

    @InjectMocks RequisitionDeliveryResolver resolver;

    @Test
    void labElectronicEnabled_resolvesToElectronic() {
        UUID clinicId = UUID.randomUUID();
        ClinicCapability cap = ClinicCapability.builder()
                .capability(CapabilityType.LAB_ELECTRONIC).enabled(true).build();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.LAB_ELECTRONIC))
                .thenReturn(Optional.of(cap));

        assertThat(resolver.resolve(clinicId)).isSameAs(electronicAdapter);
    }

    @Test
    void labElectronicDisabled_resolvesToPdfFallback() {
        UUID clinicId = UUID.randomUUID();
        ClinicCapability cap = ClinicCapability.builder()
                .capability(CapabilityType.LAB_ELECTRONIC).enabled(false).build();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.LAB_ELECTRONIC))
                .thenReturn(Optional.of(cap));

        assertThat(resolver.resolve(clinicId)).isSameAs(pdfFallbackAdapter);
    }

    @Test
    void noCapabilityRecord_resolvesToPdfFallback() {
        UUID clinicId = UUID.randomUUID();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.LAB_ELECTRONIC))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolve(clinicId)).isSameAs(pdfFallbackAdapter);
    }
}
