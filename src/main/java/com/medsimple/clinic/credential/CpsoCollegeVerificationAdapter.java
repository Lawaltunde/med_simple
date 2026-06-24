package com.medsimple.clinic.credential;

import com.medsimple.clinic.entity.VerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Automated College-registry verification (Ontario CPSO; the same seam serves CMQ/CPSBC/CPSA).
 * Active when {@code credential.verification.adapter=cpso}.
 *
 * <p>This is the single seam to fill when registry access is provisioned (mirrors the PrescribeIT
 * delivery stub): query the College's register for the registration number, confirm it is active
 * and "in good standing", and map the result to a {@link VerificationStatus}. Until then it returns
 * PENDING with a clear note rather than throwing, so an admin can still fall back to manual review.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "credential.verification.adapter", havingValue = "cpso")
public class CpsoCollegeVerificationAdapter implements PhysicianCredentialVerificationDao {

    @Override
    public VerificationResult verify(PhysicianCredential credential) {
        // TODO: call the CPSO "All Doctors Search" / provincial College registry once provider
        // access is in place, confirming the registration is active and in good standing.
        log.info("CPSO automated verification requested for registration {} ({}) — provider access pending",
                credential.registrationNumber(), credential.college());
        return new VerificationResult(
                VerificationStatus.PENDING,
                "CPSO",
                "Automated CPSO registry verification is awaiting provider onboarding; "
                        + "use manual review in the meantime.");
    }
}
