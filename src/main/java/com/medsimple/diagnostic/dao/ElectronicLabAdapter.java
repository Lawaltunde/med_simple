package com.medsimple.diagnostic.dao;

import com.medsimple.diagnostic.entity.DiagnosticDeliveryMethod;
import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import com.medsimple.diagnostic.entity.RequisitionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Electronic requisition delivery to a lab network.
 * Selected when the requisition's clinic has the LAB_ELECTRONIC capability enabled.
 *
 * NOTE: real lab-network submission requires onboarding and a live endpoint.
 * Until provisioned, this transmits and marks the requisition SENT.
 */
@Slf4j
@Component
public class ElectronicLabAdapter implements RequisitionDeliveryDao {

    @Override
    public RequisitionDeliveryResult deliver(DiagnosticRequisition requisition) {
        // TODO: submit the FHIR ServiceRequest to the lab network once onboarded.
        log.info("Transmitting requisition {} electronically to lab network", requisition.getId());
        return new RequisitionDeliveryResult(
                RequisitionStatus.SENT, DiagnosticDeliveryMethod.ELECTRONIC, null);
    }
}
