package com.medsimple.media.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real object-store media backend (S3 / Supabase Storage). Selected when
 * media.storage.adapter=s3; the local-disk adapter is the default otherwise, mirroring
 * every other module's real-adapter + fallback split.
 *
 * This is the single seam to fill when an object-store bucket is provisioned (mirrors the
 * PrescribeIT delivery stub): wire {@code store}/{@code load} to the SDK and store the
 * object key under the same /api/v1/media/{id} mediaUrl scheme so nothing downstream changes.
 * Until then it throws so a misconfiguration fails loudly instead of silently losing media.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "media.storage.adapter", havingValue = "s3")
public class ObjectStoreMediaStorageAdapter implements MediaStorageDao {

    private static final String NOT_CONFIGURED =
            "Object-store media storage (media.storage.adapter=s3) is selected but not provisioned. "
                    + "Configure the bucket + credentials, or use the default local-disk adapter.";

    @Override
    public StoredMedia store(byte[] content, String contentType, UUID ownerId) {
        throw new UnsupportedOperationException(NOT_CONFIGURED);
    }

    @Override
    public MediaContent load(UUID id) {
        throw new UnsupportedOperationException(NOT_CONFIGURED);
    }

    @Override
    public MediaContent resolve(String mediaUrl) {
        throw new UnsupportedOperationException(NOT_CONFIGURED);
    }
}
