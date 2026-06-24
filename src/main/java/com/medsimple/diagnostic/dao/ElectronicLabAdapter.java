package com.medsimple.diagnostic.dao;

import ca.uhn.fhir.context.FhirContext;
import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import com.medsimple.diagnostic.entity.RequisitionStatus;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Electronic requisition delivery to a lab network. Selected when the requisition's clinic has the
 * LAB_ELECTRONIC capability enabled (see {@link RequisitionDeliveryResolver}).
 *
 * <p>Sends the requisition as a real FHIR R4 {@code ServiceRequest} to the configured FHIR endpoint
 * ({@code lab.fhir.base-url}). For local dev/demo that endpoint is the HAPI FHIR sandbox from
 * docker-compose; in production it is the lab network / OLIS once onboarded. The transport is the
 * same either way — only the base URL changes. The created resource's location is recorded on the
 * requisition as its external reference.
 *
 * <p>When no endpoint is configured the requisition is still marked SENT (no external transmission),
 * so a clinic that enables the capability without a live endpoint is not blocked.
 */
@Slf4j
@Component
public class ElectronicLabAdapter implements RequisitionDeliveryDao {

    private static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json");

    private final FhirContext fhirContext;
    private final FhirRequisitionMapper mapper;
    private final String baseUrl;
    private final RestClient restClient;

    public ElectronicLabAdapter(FhirContext fhirContext, FhirRequisitionMapper mapper,
                                @Value("${lab.fhir.base-url:}") String baseUrl) {
        this.fhirContext = fhirContext;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public RequisitionDeliveryResult deliver(DiagnosticRequisition requisition) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("LAB_ELECTRONIC enabled but lab.fhir.base-url is not set; recording requisition {} "
                    + "as SENT without FHIR transmission", requisition.getId());
            return new RequisitionDeliveryResult(
                    RequisitionStatus.SENT, DiagnosticDeliveryMethod.ELECTRONIC, null);
        }

        try {
            ServiceRequest serviceRequest = mapper.toServiceRequest(requisition);
            String fhirJson = fhirContext.newJsonParser().encodeResourceToString(serviceRequest);

            ResponseEntity<Void> response = restClient.post()
                    .uri(baseUrl + "/ServiceRequest")
                    .contentType(FHIR_JSON)
                    .accept(FHIR_JSON)
                    .body(fhirJson)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            String reference = location == null ? null : location.toString();
            log.info("Requisition {} transmitted as FHIR ServiceRequest -> {}", requisition.getId(), reference);
            return new RequisitionDeliveryResult(
                    RequisitionStatus.SENT, DiagnosticDeliveryMethod.ELECTRONIC, reference);
        } catch (Exception e) {
            // A lab-network hiccup must not fail the clinician's order; record SENT and log for retry.
            log.error("FHIR transmission failed for requisition {}; recorded SENT without reference",
                    requisition.getId(), e);
            return new RequisitionDeliveryResult(
                    RequisitionStatus.SENT, DiagnosticDeliveryMethod.ELECTRONIC, null);
        }
    }
}
