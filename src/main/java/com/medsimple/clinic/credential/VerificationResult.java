package com.medsimple.clinic.credential;

import com.medsimple.clinic.entity.VerificationStatus;

/**
 * Outcome of a credential verification: the new status, the source that produced it
 * (e.g. CPSO, MANUAL_REVIEW), and a human-readable note for the audit trail / UI.
 */
public record VerificationResult(
        VerificationStatus status,
        String source,
        String message
) {}
