package com.medsimple.careplan.exception;

import java.util.UUID;

public class CarePlanNotFoundException extends RuntimeException {

    public CarePlanNotFoundException(UUID id) {
        super("Care plan not found: " + id);
    }
}
