package com.medsimple.referral.dao;

import com.medsimple.referral.entity.ReferralDeliveryMethod;
import com.medsimple.referral.entity.ReferralStatus;

// Outcome of submitting a referral. externalReferralId set on the Ocean path;
// documentUrl set on the PDF path.
public record ReferralDeliveryResult(
        ReferralStatus status,
        ReferralDeliveryMethod deliveryMethod,
        String externalReferralId,
        String documentUrl
) {}
