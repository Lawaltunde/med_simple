package com.medsimple.patient.exception;

import java.util.UUID;

public class PatientAlreadyRegisteredException extends RuntimeException {

    public PatientAlreadyRegisteredException(UUID patientId, UUID clinicId) {
        super("Patient " + patientId + " is already registered at clinic " + clinicId);
    }
}
