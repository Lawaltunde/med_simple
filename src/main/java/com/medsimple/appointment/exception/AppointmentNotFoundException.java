package com.medsimple.appointment.exception;

import java.util.UUID;

public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(UUID id) {
        super("Appointment not found: " + id);
    }
}
