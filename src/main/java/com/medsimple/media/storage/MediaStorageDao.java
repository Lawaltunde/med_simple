package com.medsimple.media.storage;

import java.util.UUID;

/**
 * Storage seam for patient-submitted media (photos, voice notes). Two implementations,
 * the project's standard real-adapter + fallback pattern:
 *   - {@link ObjectStoreMediaStorageAdapter} — the real object-store impl (S3/Supabase),
 *     active when media.storage.adapter=s3. A stub until credentials are provisioned.
 *   - {@link LocalDiskMediaStorageAdapter} — the default, writes under a configured dir.
 *
 * The mediaUrl returned by {@link #store} is an in-platform HTTP path (/api/v1/media/{id}),
 * which is auth-gated on read. Implementations must never return a filesystem path or a
 * world-readable URL: media may contain PHI.
 */
public interface MediaStorageDao {

    /**
     * Persist the bytes and return the id + retrievable mediaUrl + owner. Content-type
     * validation is the caller's job (see {@link MediaContentType}); the store trusts what
     * it is given. {@code ownerId} is the uploading user — stored so reads can be ownership-gated
     * (a patient reads only their own media; staff read any) before the submission is attached.
     */
    StoredMedia store(byte[] content, String contentType, UUID ownerId);

    /** Load previously stored media by id. Throws if the id is unknown. */
    MediaContent load(UUID id);

    /**
     * Resolve a persisted mediaUrl (/api/v1/media/{id}) back to its loadable content, for
     * the analysis adapters. Keeps the adapters decoupled from the storage backend.
     */
    MediaContent resolve(String mediaUrl);
}
