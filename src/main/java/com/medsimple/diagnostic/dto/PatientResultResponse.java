package com.medsimple.diagnostic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Patient view — only fields safe to show after release.
// Raw lab data is intentionally excluded; the patient sees the AI summary and the
// physician's interpretation, never the unreviewed raw result.
public record PatientResultResponse(
        UUID id,
        UUID requisitionId,
        String testType,
        String aiSummary,
        String physicianInterpretation,
        OffsetDateTime releasedToPatientAt
) {}
