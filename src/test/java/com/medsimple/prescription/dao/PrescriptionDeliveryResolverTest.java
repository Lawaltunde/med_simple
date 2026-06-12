package com.medsimple.prescription.dao;

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
class PrescriptionDeliveryResolverTest {

    @Mock ClinicCapabilityRepository capabilityRepository;
    @Mock PrescribeItDeliveryAdapter prescribeItAdapter;
    @Mock PdfFallbackDeliveryAdapter pdfFallbackAdapter;

    @InjectMocks PrescriptionDeliveryResolver resolver;

    @Test
    void prescribeItEnabled_resolvesToPrescribeItAdapter() {
        UUID clinicId = UUID.randomUUID();
        ClinicCapability cap = ClinicCapability.builder()
                .capability(CapabilityType.PRESCRIBIT).enabled(true).build();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.PRESCRIBIT))
                .thenReturn(Optional.of(cap));

        assertThat(resolver.resolve(clinicId)).isSameAs(prescribeItAdapter);
    }

    @Test
    void prescribeItDisabled_resolvesToPdfFallback() {
        UUID clinicId = UUID.randomUUID();
        ClinicCapability cap = ClinicCapability.builder()
                .capability(CapabilityType.PRESCRIBIT).enabled(false).build();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.PRESCRIBIT))
                .thenReturn(Optional.of(cap));

        assertThat(resolver.resolve(clinicId)).isSameAs(pdfFallbackAdapter);
    }

    @Test
    void noCapabilityRecord_resolvesToPdfFallback() {
        UUID clinicId = UUID.randomUUID();
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.PRESCRIBIT))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolve(clinicId)).isSameAs(pdfFallbackAdapter);
    }
}
