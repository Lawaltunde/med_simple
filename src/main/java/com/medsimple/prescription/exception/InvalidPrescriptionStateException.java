package com.medsimple.prescription.exception;

public class InvalidPrescriptionStateException extends RuntimeException {

    public InvalidPrescriptionStateException(String message) {
        super(message);
    }
}
