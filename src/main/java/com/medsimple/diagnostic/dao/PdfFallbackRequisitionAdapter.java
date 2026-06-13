package com.medsimple.diagnostic.dao;

import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import com.medsimple.diagnostic.entity.RequisitionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback delivery: render a printable requisition PDF the patient takes to any lab.
 * Used whenever the clinic does not have LAB_ELECTRONIC enabled — every clinic can
 * always order tests, regardless of lab-network onboarding.
 */
@Slf4j
@Component
public class PdfFallbackRequisitionAdapter implements RequisitionDeliveryDao {

    @Override
    public RequisitionDeliveryResult deliver(DiagnosticRequisition requisition) {
        // TODO: render the requisition PDF and upload to object storage (Supabase).
        String documentUrl = "storage://requisitions/" + requisition.getId() + ".pdf";
        log.info("Generated PDF requisition {} at {}", requisition.getId(), documentUrl);
        return new RequisitionDeliveryResult(
                RequisitionStatus.SENT, DiagnosticDeliveryMethod.PDF_PRINT, documentUrl);
    }
}
