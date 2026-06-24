package com.medsimple.media.storage;

import com.medsimple.media.exception.MediaNotFoundException;

import java.util.UUID;

/**
 * The mediaUrl scheme: /api/v1/media/{id}. One place owns parsing and building so the
 * controller path, the stored value, and the adapters' resolve logic can never drift.
 * Deterministic string work — kept out of latent space.
 */
public final class MediaUrls {

    private MediaUrls() {}

    public static final String PREFIX = "/api/v1/media/";

    public static String forId(UUID id) {
        return PREFIX + id;
    }

    /**
     * Extract the media id from a stored mediaUrl. Tolerates an absolute URL
     * (https://host/api/v1/media/{id}) so the same value works if a future deployment
     * stores absolute URLs. Throws MediaNotFoundException for anything else.
     */
    public static UUID idFrom(String mediaUrl) {
        if (mediaUrl == null) throw new MediaNotFoundException(null);
        int at = mediaUrl.indexOf(PREFIX);
        if (at < 0) throw new IllegalArgumentException("Not a media url: " + mediaUrl);
        String tail = mediaUrl.substring(at + PREFIX.length());
        int slash = tail.indexOf('/');
        if (slash >= 0) tail = tail.substring(0, slash);
        int q = tail.indexOf('?');
        if (q >= 0) tail = tail.substring(0, q);
        try {
            return UUID.fromString(tail);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Not a media url: " + mediaUrl);
        }
    }
}
