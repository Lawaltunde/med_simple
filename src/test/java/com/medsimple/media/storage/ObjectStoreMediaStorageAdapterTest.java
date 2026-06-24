package com.medsimple.media.storage;

import com.medsimple.media.exception.MediaNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the object-store adapter wiring with a mocked S3 client — a free gate test (no real
 * bucket, no network). The actual Supabase round-trip is covered by the live e2e on :8081.
 */
@ExtendWith(MockitoExtension.class)
class ObjectStoreMediaStorageAdapterTest {

    @Mock S3Client s3;

    private ObjectStoreMediaStorageAdapter adapter() {
        return new ObjectStoreMediaStorageAdapter(s3, "medsimple-media");
    }

    @Test
    void store_putsBytesWithContentTypeAndOwnerMetadata_andReturnsInPlatformUrl() {
        UUID owner = UUID.randomUUID();

        StoredMedia stored = adapter().store(new byte[]{1, 2, 3}, "image/png", owner);

        ArgumentCaptor<PutObjectRequest> req = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(req.capture(), any(RequestBody.class));
        assertThat(req.getValue().bucket()).isEqualTo("medsimple-media");
        assertThat(req.getValue().key()).isEqualTo("media/" + stored.id());
        assertThat(req.getValue().contentType()).isEqualTo("image/png");
        assertThat(req.getValue().metadata()).containsEntry("owner-id", owner.toString());

        // Never a bucket URL or filesystem path — always the auth-gated in-platform path.
        assertThat(stored.mediaUrl()).isEqualTo("/api/v1/media/" + stored.id());
        assertThat(stored.ownerId()).isEqualTo(owner);
        assertThat(stored.contentType()).isEqualTo("image/png");
    }

    @Test
    void load_returnsBytesContentTypeAndOwnerFromObjectMetadata() throws Exception {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        GetObjectResponse resp = GetObjectResponse.builder()
                .contentType("audio/webm")
                .metadata(Map.of("owner-id", owner.toString()))
                .build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(resp, new byte[]{9, 8, 7}));

        MediaContent content = adapter().load(id);

        assertThat(content.contentType()).isEqualTo("audio/webm");
        assertThat(content.ownerId()).isEqualTo(owner);
        assertThat(content.resource().getContentAsByteArray()).containsExactly(9, 8, 7);
    }

    @Test
    void load_resourceCarriesFilenameWithExtension_soWhisperVisionCanDetectFormat() {
        // Regression: a bare ByteArrayResource has no filename, so OpenAI rejected the audio with
        // "Unrecognized file format". The resource must expose {id}.{ext}.
        UUID id = UUID.randomUUID();
        GetObjectResponse resp = GetObjectResponse.builder()
                .contentType("audio/webm")
                .metadata(Map.of())
                .build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(resp, new byte[]{1, 2}));

        MediaContent content = adapter().load(id);

        assertThat(content.resource().getFilename()).isEqualTo(id + ".webm");
    }

    @Test
    void load_unknownKey_throwsMediaNotFound() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no such key").build());

        assertThatThrownBy(() -> adapter().load(UUID.randomUUID()))
                .isInstanceOf(MediaNotFoundException.class);
    }

    @Test
    void load_missingOwnerMetadata_yieldsNullOwner() throws Exception {
        GetObjectResponse resp = GetObjectResponse.builder()
                .contentType("image/jpeg")
                .metadata(Map.of())
                .build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(resp, new byte[]{1}));

        MediaContent content = adapter().load(UUID.randomUUID());

        assertThat(content.ownerId()).isNull();
        assertThat(content.resource().getContentAsByteArray()).containsExactly(1);
    }

    @Test
    void resolve_parsesMediaUrlThenLoads() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        GetObjectResponse resp = GetObjectResponse.builder()
                .contentType("image/png")
                .metadata(Map.of("owner-id", owner.toString()))
                .build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(resp, new byte[]{4}));

        MediaContent content = adapter().resolve("/api/v1/media/" + id);

        assertThat(content.ownerId()).isEqualTo(owner);

        ArgumentCaptor<GetObjectRequest> req = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObjectAsBytes(req.capture());
        assertThat(req.getValue().key()).isEqualTo("media/" + id);
    }
}
