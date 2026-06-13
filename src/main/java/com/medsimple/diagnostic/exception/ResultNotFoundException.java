package com.medsimple.diagnostic.exception;

import java.util.UUID;

public class ResultNotFoundException extends RuntimeException {

    public ResultNotFoundException(UUID id) {
        super("Diagnostic result not found: " + id);
    }
}
