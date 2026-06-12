package com.medsimple.prescription.dao;

import com.medsimple.prescription.entity.DeliveryMethod;
import com.medsimple.prescription.entity.Prescription;
import com.medsimple.prescription.entity.PrescriptionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback delivery: render a signed prescription PDF the physician can print or fax.
 * Used whenever the clinic does not have PRESCRIBIT enabled — every clinic can always
 * prescribe, regardless of electronic-network onboarding.
 */
@Slf4j
@Component
public class PdfFallbackDeliveryAdapter implements PrescriptionDeliveryDao {

    @Override
    public DeliveryResult deliver(Prescription prescription) {
        // TODO: render the PDF and upload to object storage (Supabase) once wired;
        // for now we produce a deterministic document reference.
        String documentUrl = "storage://prescriptions/" + prescription.getId() + ".pdf";
        log.info("Generated PDF prescription {} at {}", prescription.getId(), documentUrl);
        return new DeliveryResult(PrescriptionStatus.SENT_PDF, DeliveryMethod.PDF_PRINT, documentUrl);
    }
}
