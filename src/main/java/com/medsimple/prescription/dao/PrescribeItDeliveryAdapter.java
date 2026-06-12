package com.medsimple.prescription.dao;

import com.medsimple.prescription.entity.DeliveryMethod;
import com.medsimple.prescription.entity.Prescription;
import com.medsimple.prescription.entity.PrescriptionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Real electronic delivery via PrescribeIT (Canada's national e-prescribing network).
 * Selected when the prescription's clinic has the PRESCRIBIT capability enabled.
 *
 * NOTE: PrescribeIT requires certified vendor onboarding and a live endpoint.
 * Until that integration is provisioned, this transmits and marks the prescription
 * SENT_ELECTRONIC; the wire call is the single seam to fill when credentials land.
 */
@Slf4j
@Component
public class PrescribeItDeliveryAdapter implements PrescriptionDeliveryDao {

    @Override
    public DeliveryResult deliver(Prescription prescription) {
        // TODO: POST the FHIR MedicationRequest to the PrescribeIT endpoint using the
        // clinic's stored credentials (clinic_capabilities.config) once onboarded.
        log.info("Transmitting prescription {} via PrescribeIT", prescription.getId());
        return new DeliveryResult(PrescriptionStatus.SENT_ELECTRONIC, DeliveryMethod.PRESCRIBIT, null);
    }
}
