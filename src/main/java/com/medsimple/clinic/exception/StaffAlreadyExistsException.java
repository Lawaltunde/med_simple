package com.medsimple.clinic.exception;

public class StaffAlreadyExistsException extends RuntimeException {

    public StaffAlreadyExistsException(String email) {
        super("Staff member already exists at this clinic: " + email);
    }
}
