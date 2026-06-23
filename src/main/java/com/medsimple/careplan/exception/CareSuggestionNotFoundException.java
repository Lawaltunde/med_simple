package com.medsimple.careplan.exception;

import java.util.UUID;

public class CareSuggestionNotFoundException extends RuntimeException {

    public CareSuggestionNotFoundException(UUID id) {
        super("Care suggestion not found: " + id);
    }
}
