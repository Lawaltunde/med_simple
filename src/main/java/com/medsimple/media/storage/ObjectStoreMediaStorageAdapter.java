package com.medsimple.media.storage;

import com.medsimple.media.exception.MediaNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;
import java.util.UUID;

/**
 * Real object-store media backend (S3 / Supabase Storage). Selected when
 * media.storage.adapter=s3; the local-disk adapter is the default otherwise, mirroring every
 * other module's real-adapter + fallback split.
 *
 * <p>Mirrors the local-disk layout one-for-one: the bytes go to the object {@code media/{id}}, and
 * the content type + owning user ride along as S3 object metadata (the equivalent of the local
 * sidecar file), so {@link #load} returns the right Content-Type and owner without a separate
 * lookup table. The id is a server-generated UUID and the mediaUrl stays /api/v1/media/{id}, so
 * nothing downstream changes when this backend is swapped in.
 *
 * <p>PHI: only the bytes and a content-type/owner metadata pair leave for the bucket. No patient
 * identifiers in the key, and nothing about the media is logged.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "media.storage.adapter", havingValue = "s3")
public class ObjectStoreMediaStorageAdapter implements MediaStorageDao {

    // S3 lowercases user-metadata keys; keep it lowercase here so store/load round-trip cleanly.
    private static final String OWNER_META = "owner-id";

    private final S3Client s3;
    private final String bucket;

    public ObjectStoreMediaStorageAdapter(S3Client s3,
                                          @Value("${media.storage.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public StoredMedia store(byte[] content, String contentType, UUID ownerId) {
        UUID id = UUID.randomUUID();
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key(id))
                        .contentType(contentType)
                        .metadata(Map.of(OWNER_META, ownerId == null ? "" : ownerId.toString()))
                        .build(),
                RequestBody.fromBytes(content));
        return new StoredMedia(id, MediaUrls.forId(id), contentType, ownerId);
    }

    @Override
    public MediaContent load(UUID id) {
        ResponseBytes<GetObjectResponse> obj;
        try {
            obj = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key(id))
                    .build());
        } catch (NoSuchKeyException e) {
            throw new MediaNotFoundException(id);
        }
        GetObjectResponse meta = obj.response();
        String owner = meta.metadata().get(OWNER_META);
        UUID ownerId = (owner != null && !owner.isBlank()) ? UUID.fromString(owner) : null;
        return new MediaContent(new ByteArrayResource(obj.asByteArray()), meta.contentType(), ownerId);
    }

    @Override
    public MediaContent resolve(String mediaUrl) {
        return load(MediaUrls.idFrom(mediaUrl));
    }

    private String key(UUID id) {
        return "media/" + id;
    }
}
