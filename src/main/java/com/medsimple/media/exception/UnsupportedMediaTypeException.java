package com.medsimple.media.exception;

/** Upload rejected: the content type is not in the accepted image/audio allow-list. Maps to 415. */
public class UnsupportedMediaTypeException extends RuntimeException {
    public UnsupportedMediaTypeException(String contentType) {
        super("Unsupported media type: " + (contentType == null ? "(none)" : contentType));
    }
}
