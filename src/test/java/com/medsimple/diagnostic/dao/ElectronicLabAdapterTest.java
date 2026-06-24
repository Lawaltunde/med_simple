package com.medsimple.diagnostic.dao;

import ca.uhn.fhir.context.FhirContext;
import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import com.medsimple.diagnostic.entity.RequisitionStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate test for the no-endpoint branch (free, no server). The real FHIR transmission to the HAPI
 * sandbox is covered by the live e2e.
 */
class ElectronicLabAdapterTest {

    @Test
    void whenNoFhirEndpointConfigured_recordsSentWithoutTransmitting() {
        ElectronicLabAdapter adapter = new ElectronicLabAdapter(
                FhirContext.forR4(), new FhirRequisitionMapper(), "");

        DiagnosticRequisition req = DiagnosticRequisition.builder().id(UUID.randomUUID()).build();
        RequisitionDeliveryResult result = adapter.deliver(req);

        assertThat(result.status()).isEqualTo(RequisitionStatus.SENT);
        assertThat(result.deliveryMethod()).isEqualTo(DiagnosticDeliveryMethod.ELECTRONIC);
        assertThat(result.documentUrl()).isNull();
    }
}
