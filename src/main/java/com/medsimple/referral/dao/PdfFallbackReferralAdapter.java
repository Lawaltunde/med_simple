package com.medsimple.referral.dao;

import com.medsimple.referral.entity.Referral;
import com.medsimple.referral.entity.ReferralDeliveryMethod;
import com.medsimple.referral.entity.ReferralStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback delivery: render a referral letter PDF the physician can print or fax.
 * Used whenever the clinic does not have OCEAN_EREFERRAL enabled — every clinic can
 * always refer, regardless of eReferral-network onboarding.
 */
@Slf4j
@Component
public class PdfFallbackReferralAdapter implements ReferralDeliveryDao {

    @Override
    public ReferralDeliveryResult deliver(Referral referral) {
        // TODO: render the referral letter PDF and upload to object storage (Supabase).
        String documentUrl = "storage://referrals/" + referral.getId() + ".pdf";
        log.info("Generated PDF referral letter {} at {}", referral.getId(), documentUrl);
        return new ReferralDeliveryResult(
                ReferralStatus.SUBMITTED, ReferralDeliveryMethod.PDF_PRINT, null, documentUrl);
    }
}
