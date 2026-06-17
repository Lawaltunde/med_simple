package com.medsimple.auth.exception;

// Raised when an identifier exceeds the allowed failed-login attempts. Maps to HTTP 429.
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
