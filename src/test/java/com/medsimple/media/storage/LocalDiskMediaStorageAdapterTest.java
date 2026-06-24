package com.medsimple.media.storage;

import com.medsimple.media.exception.MediaNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class LocalDiskMediaStorageAdapterTest {

    @TempDir
    Path tmp;

    private LocalDiskMediaStorageAdapter adapter() {
        return new LocalDiskMediaStorageAdapter(tmp.toString());
    }

    @Test
    void store_thenLoad_roundTripsBytesTypeAndOwner() throws IOException {
        LocalDiskMediaStorageAdapter adapter = adapter();
        UUID owner = UUID.randomUUID();
        byte[] bytes = "hello-image".getBytes(StandardCharsets.UTF_8);

        StoredMedia stored = adapter.store(bytes, "image/png", owner);

        assertThat(stored.mediaUrl()).isEqualTo("/api/v1/media/" + stored.id());
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.ownerId()).isEqualTo(owner);

        MediaContent loaded = adapter.load(stored.id());
        assertThat(loaded.contentType()).isEqualTo("image/png");
        assertThat(loaded.ownerId()).isEqualTo(owner);
        assertThat(loaded.resource().getContentAsByteArray()).isEqualTo(bytes);
    }

    @Test
    void resolve_acceptsThePersistedMediaUrl() throws IOException {
        LocalDiskMediaStorageAdapter adapter = adapter();
        StoredMedia stored = adapter.store("audio".getBytes(), "audio/webm", UUID.randomUUID());

        MediaContent resolved = adapter.resolve(stored.mediaUrl());

        assertThat(resolved.contentType()).isEqualTo("audio/webm");
        assertThat(resolved.resource().getContentAsByteArray()).isEqualTo("audio".getBytes());
    }

    @Test
    void load_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> adapter().load(UUID.randomUUID()))
                .isInstanceOf(MediaNotFoundException.class);
    }

    @Test
    void mediaUrl_idRoundTrips_andRejectsTraversalGarbage() {
        UUID id = UUID.randomUUID();
        assertThat(MediaUrls.idFrom(MediaUrls.forId(id))).isEqualTo(id);
        // A crafted value that is not /api/v1/media/{uuid} cannot be parsed into an id,
        // so it can never reach a file on disk.
        assertThatThrownBy(() -> MediaUrls.idFrom("/api/v1/media/../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_distinctIdsForDistinctUploads() {
        LocalDiskMediaStorageAdapter adapter = adapter();
        StoredMedia a = adapter.store("a".getBytes(), "image/jpeg", UUID.randomUUID());
        StoredMedia b = adapter.store("b".getBytes(), "image/jpeg", UUID.randomUUID());
        assertThat(a.id()).isNotEqualTo(b.id());
    }
}
