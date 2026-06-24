package com.medsimple.clinic.credential;

import com.medsimple.clinic.entity.VerificationStatus;
import org.springframework.stereotype.Component;

/**
 * Default verification path: a clinic admin confirms the physician's registration by hand against
 * the College's public register (e.g. CPSO "All Doctors Search"), and the platform records that
 * attestation. This is how credentialing works today at most clinics, and it keeps the verify
 * action fully functional before any automated registry integration exists.
 *
 * <p>Active unless {@code credential.verification.adapter=cpso} selects the automated adapter.
 */
@Component
public class ManualReviewCredentialVerificationAdapter implements PhysicianCredentialVerificationDao {

    @Override
    public VerificationResult verify(PhysicianCredential credential) {
        String college = credential.college() == null ? "the College" : credential.college();
        return new VerificationResult(
                VerificationStatus.VERIFIED,
                "MANUAL_REVIEW",
                "Confirmed by clinic admin against the " + college + " public register.");
    }
}
