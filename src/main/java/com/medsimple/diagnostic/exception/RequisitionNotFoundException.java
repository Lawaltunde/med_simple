package com.medsimple.diagnostic.exception;

import java.util.UUID;

public class RequisitionNotFoundException extends RuntimeException {

    public RequisitionNotFoundException(UUID id) {
        super("Diagnostic requisition not found: " + id);
    }
}
