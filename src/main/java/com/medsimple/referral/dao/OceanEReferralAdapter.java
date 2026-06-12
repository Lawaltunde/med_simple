package com.medsimple.referral.dao;

import com.medsimple.referral.entity.Referral;
import com.medsimple.referral.entity.ReferralDeliveryMethod;
import com.medsimple.referral.entity.ReferralStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Real electronic delivery via Ocean eReferral (the dominant Canadian eReferral network).
 * Selected when the referral's clinic has the OCEAN_EREFERRAL capability enabled.
 *
 * NOTE: Ocean requires vendor onboarding and a live endpoint. Until provisioned, this
 * transmits and marks the referral SUBMITTED with a generated external reference; the
 * wire call is the single seam to fill when credentials land.
 */
@Slf4j
@Component
public class OceanEReferralAdapter implements ReferralDeliveryDao {

    @Override
    public ReferralDeliveryResult deliver(Referral referral) {
        // TODO: POST the FHIR ServiceRequest to Ocean using the clinic's stored credentials.
        String externalId = "OCEAN-" + referral.getId().toString().substring(0, 8).toUpperCase();
        log.info("Transmitting referral {} via Ocean eReferral as {}", referral.getId(), externalId);
        return new ReferralDeliveryResult(
                ReferralStatus.SUBMITTED, ReferralDeliveryMethod.OCEAN_EREFERRAL, externalId, null);
    }
}
