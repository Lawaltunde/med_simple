package com.medsimple.diagnostic.dao;

import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import com.medsimple.diagnostic.entity.RequisitionUrgency;
import com.medsimple.shared.entity.User;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FhirRequisitionMapperTest {

    private final FhirRequisitionMapper mapper = new FhirRequisitionMapper();

    private DiagnosticRequisition sampleRequisition() {
        User patient = User.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe").build();
        User physician = User.builder().id(UUID.randomUUID()).firstName("Dana").lastName("Okafor").build();
        return DiagnosticRequisition.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .orderingPhysician(physician)
                .testType("Complete Blood Count")
                .testCodes(List.of("58410-2"))
                .clinicalIndication("Fatigue, rule out anemia")
                .urgency(RequisitionUrgency.URGENT)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void mapsCoreServiceRequestFields() {
        DiagnosticRequisition req = sampleRequisition();

        ServiceRequest sr = mapper.toServiceRequest(req);

        assertThat(sr.getStatus()).isEqualTo(ServiceRequest.ServiceRequestStatus.ACTIVE);
        assertThat(sr.getIntent()).isEqualTo(ServiceRequest.ServiceRequestIntent.ORDER);
        assertThat(sr.getPriority()).isEqualTo(ServiceRequest.ServiceRequestPriority.URGENT);
        assertThat(sr.getCode().getText()).isEqualTo("Complete Blood Count");
        assertThat(sr.getCode().getCodingFirstRep().getSystem()).isEqualTo("http://loinc.org");
        assertThat(sr.getCode().getCodingFirstRep().getCode()).isEqualTo("58410-2");
        assertThat(sr.getReasonCodeFirstRep().getText()).isEqualTo("Fatigue, rule out anemia");
        assertThat(sr.getAuthoredOn()).isNotNull();
    }

    @Test
    void carriesCorrelationIdentifierAndLogicalReferences() {
        DiagnosticRequisition req = sampleRequisition();

        ServiceRequest sr = mapper.toServiceRequest(req);

        // Our requisition id rides along so the lab response can be matched back.
        assertThat(sr.getIdentifierFirstRep().getValue()).isEqualTo(req.getId().toString());
        // Subject + requester are logical references (identifier + display), no literal URL.
        assertThat(sr.getSubject().getDisplay()).isEqualTo("Jane Doe");
        assertThat(sr.getSubject().getIdentifier().getValue()).isEqualTo(req.getPatient().getId().toString());
        assertThat(sr.getRequester().getDisplay()).contains("Okafor");
    }

    @Test
    void nullUrgencyDefaultsToRoutine() {
        DiagnosticRequisition req = sampleRequisition();
        req.setUrgency(null);

        assertThat(mapper.toServiceRequest(req).getPriority())
                .isEqualTo(ServiceRequest.ServiceRequestPriority.ROUTINE);
    }
}
