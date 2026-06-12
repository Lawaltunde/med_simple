package com.medsimple.referral.dao;

import com.medsimple.referral.entity.Referral;

// One implementation per delivery channel; selected at runtime per clinic
// by ReferralDeliveryResolver based on clinic_capabilities.
public interface ReferralDeliveryDao {

    ReferralDeliveryResult deliver(Referral referral);
}
