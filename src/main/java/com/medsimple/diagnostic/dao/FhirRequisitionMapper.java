package com.medsimple.diagnostic.dao;

import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import com.medsimple.diagnostic.entity.RequisitionUrgency;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Maps an internal {@link DiagnosticRequisition} to a FHIR R4 {@code ServiceRequest} — the same
 * resource a lab network / OLIS consumes (the FHIR table-mapping in CLAUDE.md:
 * diagnostic_requisitions → ServiceRequest). Deterministic, same input → same resource, so it is
 * pure mapping with no I/O and is unit-tested without a server.
 */
@Component
public class FhirRequisitionMapper {

    private static final String SYS_REQUISITION = "https://medsimple.health/fhir/requisition";
    private static final String SYS_PATIENT = "https://medsimple.health/fhir/patient";
    private static final String SYS_PRACTITIONER = "https://medsimple.health/fhir/practitioner";

    public ServiceRequest toServiceRequest(DiagnosticRequisition req) {
        ServiceRequest sr = new ServiceRequest();
        sr.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        sr.setPriority(mapPriority(req.getUrgency()));

        // Our requisition id as a business identifier so the lab response can be correlated back.
        sr.addIdentifier(new Identifier().setSystem(SYS_REQUISITION).setValue(req.getId().toString()));

        // The ordered test: free-text test type plus any coded entries (LOINC).
        CodeableConcept code = new CodeableConcept().setText(req.getTestType());
        if (req.getTestCodes() != null) {
            for (String c : req.getTestCodes()) {
                code.addCoding(new Coding().setSystem("http://loinc.org").setCode(c));
            }
        }
        sr.setCode(code);

        // Subject + requester as logical references (identifier + display) — the lab resolves them
        // against its own patient/provider directory.
        sr.setSubject(new Reference()
                .setIdentifier(new Identifier().setSystem(SYS_PATIENT)
                        .setValue(req.getPatient().getId().toString()))
                .setDisplay(fullName(req.getPatient().getFirstName(), req.getPatient().getLastName())));
        sr.setRequester(new Reference()
                .setIdentifier(new Identifier().setSystem(SYS_PRACTITIONER)
                        .setValue(req.getOrderingPhysician().getId().toString()))
                .setDisplay("Dr. " + req.getOrderingPhysician().getLastName()));

        if (req.getClinicalIndication() != null && !req.getClinicalIndication().isBlank()) {
            sr.addReasonCode(new CodeableConcept().setText(req.getClinicalIndication()));
        }
        if (req.getCreatedAt() != null) {
            sr.setAuthoredOn(Date.from(req.getCreatedAt().toInstant()));
        }
        return sr;
    }

    private ServiceRequest.ServiceRequestPriority mapPriority(RequisitionUrgency urgency) {
        if (urgency == null) {
            return ServiceRequest.ServiceRequestPriority.ROUTINE;
        }
        return switch (urgency) {
            case ROUTINE -> ServiceRequest.ServiceRequestPriority.ROUTINE;
            case URGENT -> ServiceRequest.ServiceRequestPriority.URGENT;
            case STAT -> ServiceRequest.ServiceRequestPriority.STAT;
        };
    }

    private String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }
}
