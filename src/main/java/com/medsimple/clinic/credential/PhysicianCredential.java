package com.medsimple.clinic.credential;

/**
 * The physician credential presented for verification against a provincial College registry.
 * PHI-free: a professional registration is public information, not patient data.
 */
public record PhysicianCredential(
        String college,
        String registrationNumber,
        String province,
        String firstName,
        String lastName
) {}
