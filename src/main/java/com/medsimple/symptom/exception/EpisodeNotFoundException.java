package com.medsimple.symptom.exception;

import java.util.UUID;

public class EpisodeNotFoundException extends RuntimeException {

    public EpisodeNotFoundException(UUID id) {
        super("Symptom episode not found: " + id);
    }
}
