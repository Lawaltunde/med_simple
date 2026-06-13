package com.medsimple.media.exception;

import java.util.UUID;

public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(UUID id) {
        super("Symptom submission not found: " + id);
    }
}
