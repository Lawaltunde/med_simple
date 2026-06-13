package com.medsimple.diagnostic.exception;

import java.util.UUID;

// Thrown when a patient-scoped read targets a result that has not been released yet.
// The PHIPA gate — a result exists but the patient is not permitted to see it.
public class ResultNotReleasedException extends RuntimeException {

    public ResultNotReleasedException(UUID id) {
        super("Diagnostic result has not been released to the patient: " + id);
    }
}
