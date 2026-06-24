package com.medsimple.media.exception;

/** Upload rejected: the file exceeds the configured maximum size. Maps to 413. */
public class MediaTooLargeException extends RuntimeException {
    public MediaTooLargeException(long size, long max) {
        super("Media too large: " + size + " bytes (max " + max + ")");
    }
}
