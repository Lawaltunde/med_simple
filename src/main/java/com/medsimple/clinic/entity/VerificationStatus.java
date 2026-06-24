package com.medsimple.clinic.entity;

/**
 * State of a physician's College-registration verification.
 *
 * <p>UNVERIFIED — credentials captured, not yet checked. A new physician starts here.
 * <p>PENDING — an automated registry lookup is in flight / awaiting provider onboarding.
 * <p>VERIFIED — confirmed registered in good standing (by the College API or manual review).
 * <p>REJECTED — the registration could not be confirmed; the physician must not act clinically.
 */
public enum VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED
}
