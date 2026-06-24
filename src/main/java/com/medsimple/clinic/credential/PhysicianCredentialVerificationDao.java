package com.medsimple.clinic.credential;

/**
 * Verifies a physician's College registration. The project's standard real-adapter + fallback
 * seam: the real {@link CpsoCollegeVerificationAdapter} queries the provincial College registry
 * (when provider access is provisioned), and the default {@link ManualReviewCredentialVerificationAdapter}
 * records a clinic admin's manual confirmation against the public register. Callers depend only on
 * this interface, so connecting the real registry later changes no caller.
 */
public interface PhysicianCredentialVerificationDao {

    VerificationResult verify(PhysicianCredential credential);
}
