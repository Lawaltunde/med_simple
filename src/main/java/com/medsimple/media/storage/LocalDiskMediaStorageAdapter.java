package com.medsimple.media.storage;

import com.medsimple.media.exception.MediaNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Default media storage: files on local disk under {@code media.storage.local.dir}.
 * Active unless media.storage.adapter=s3 selects the object-store adapter.
 *
 * Layout per upload: a content file {id}.{ext} and a sidecar {id}.meta holding
 * "{contentType}\n{ownerId}", so {@link #load} returns the right Content-Type and owner
 * without sniffing. The id is a server-generated UUID and is parsed as a UUID on every read,
 * so a crafted mediaUrl cannot escape the storage dir (no path traversal).
 *
 * PHI: only bytes hit disk — no patient identifiers in the filename or anywhere here, and
 * nothing about the media is logged.
 */
@Slf4j
@Component
public class LocalDiskMediaStorageAdapter implements MediaStorageDao {

    private final Path root;

    public LocalDiskMediaStorageAdapter(
            @Value("${media.storage.local.dir:${java.io.tmpdir}/medsimple-media}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create media storage dir " + root, e);
        }
        log.info("Local media storage at {}", root);
    }

    @Override
    public StoredMedia store(byte[] content, String contentType, UUID ownerId) {
        UUID id = UUID.randomUUID();
        String ext = MediaContentType.extensionFor(contentType);
        try {
            Files.write(contentFile(id, ext), content);
            Files.writeString(metaFile(id), contentType + "\n" + ownerId, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store media " + id, e);
        }
        return new StoredMedia(id, MediaUrls.forId(id), contentType, ownerId);
    }

    @Override
    public MediaContent load(UUID id) {
        Path meta = metaFile(id);
        if (!Files.exists(meta)) throw new MediaNotFoundException(id);
        String contentType;
        UUID ownerId;
        try {
            String[] lines = Files.readString(meta, StandardCharsets.UTF_8).split("\n", 2);
            contentType = lines[0].trim();
            ownerId = lines.length > 1 && !lines[1].isBlank() ? UUID.fromString(lines[1].trim()) : null;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read media meta " + id, e);
        }
        Path content = contentFile(id, MediaContentType.extensionFor(contentType));
        if (!Files.exists(content)) throw new MediaNotFoundException(id);
        return new MediaContent(new PathResource(content), contentType, ownerId);
    }

    @Override
    public MediaContent resolve(String mediaUrl) {
        return load(MediaUrls.idFrom(mediaUrl));
    }

    private Path contentFile(UUID id, String ext) {
        return root.resolve(id + "." + ext);
    }

    private Path metaFile(UUID id) {
        return root.resolve(id + ".meta");
    }
}
