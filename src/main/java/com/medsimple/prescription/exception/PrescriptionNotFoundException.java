package com.medsimple.prescription.exception;

import java.util.UUID;

public class PrescriptionNotFoundException extends RuntimeException {

    public PrescriptionNotFoundException(UUID id) {
        super("Prescription not found: " + id);
    }
}
