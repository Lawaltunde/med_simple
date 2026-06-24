package com.medsimple.patient.exception;

/**
 * A submitted health-card number failed validation (format or check digit) for its province.
 * Mapped to HTTP 400 — the card is rejected before it is ever encrypted or stored.
 */
public class InvalidHealthCardException extends RuntimeException {
    public InvalidHealthCardException(String message) {
        super(message);
    }
}
