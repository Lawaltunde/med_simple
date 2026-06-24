package com.medsimple.media.storage;

import java.util.UUID;

/**
 * The result of storing an uploaded media file: its opaque id and the retrievable
 * mediaUrl persisted on the submission. The mediaUrl is an in-platform HTTP path
 * (/api/v1/media/{id}) — never a filesystem path and never world-readable.
 */
public record StoredMedia(UUID id, String mediaUrl, String contentType, UUID ownerId) {}
