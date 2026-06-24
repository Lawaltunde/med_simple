package com.medsimple.media.exception;

import java.util.UUID;

/** A media id was requested that is not in storage. Maps to 404. */
public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(UUID id) {
        super("Media not found: " + id);
    }
}
