package com.medsimple.clinic.exception;

/**
 * A physician was onboarded with missing or malformed College credentials. Mapped to HTTP 400 —
 * the credential is rejected before the staff record is created.
 */
public class InvalidPhysicianCredentialException extends RuntimeException {
    public InvalidPhysicianCredentialException(String message) {
        super(message);
    }
}
