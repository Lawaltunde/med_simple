package com.medsimple.media.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.media.exception.MediaTooLargeException;
import com.medsimple.media.exception.UnsupportedMediaTypeException;
import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaContentType;
import com.medsimple.media.storage.MediaStorageDao;
import com.medsimple.media.storage.MediaUrls;
import com.medsimple.media.storage.StoredMedia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Upload + retrieval orchestration for patient media. Validates content type and size,
 * delegates persistence to the {@link MediaStorageDao}, and audits both directions.
 *
 * Compliance (PHIPA):
 *   - Upload is attributed to the authenticated uploader; only metadata (id, content type,
 *     byte count) is audited — never the bytes, never PHI.
 *   - Read is ownership-gated: the media stores its owning user at upload time, and
 *     {@link #download} enforces AccessGuard.requirePatientSelfOrStaff against that owner.
 *     A patient reaches only their own media; clinic staff reach any. There is no
 *     unauthenticated path — the endpoint sits behind the JWT filter.
 */
@Slf4j
@Service
public class MediaStorageService {

    private final MediaStorageDao storage;
    private final AuditService auditService;
    private final AccessGuard accessGuard;
    private final long maxBytes;

    public MediaStorageService(MediaStorageDao storage,
                               AuditService auditService,
                               AccessGuard accessGuard,
                               @Value("${media.storage.max-bytes:15728640}") long maxBytes) {
        this.storage = storage;
        this.auditService = auditService;
        this.accessGuard = accessGuard;
        this.maxBytes = maxBytes;
    }

    /**
     * Validate and store an uploaded file. The uploader must be authenticated; the upload
     * is attributed to them, and they become the media owner for the read gate.
     */
    public StoredMedia upload(byte[] content, String contentType, String ipAddress) {
        if (!MediaContentType.isAllowed(contentType)) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        if (content == null || content.length == 0) {
            throw new UnsupportedMediaTypeException(contentType); // empty body — treat as a bad upload
        }
        if (content.length > maxBytes) {
            throw new MediaTooLargeException(content.length, maxBytes);
        }

        UUID uploader = accessGuard.currentUserId();
        StoredMedia stored = storage.store(content, contentType, uploader);

        // Audit metadata only — uploader id, media id, type and size. No bytes, no PHI.
        auditService.log(uploader, "MEDIA_UPLOADED", "symptom_media", stored.id(), ipAddress,
                contentType + ";" + content.length + "b");

        return stored;
    }

    /**
     * Load media for retrieval, gated on the stored owner. A patient may read only media they
     * uploaded; staff may read any. The read is audited as an access event.
     */
    public MediaContent download(UUID id, String ipAddress) {
        MediaContent content = storage.load(id);
        accessGuard.requirePatientSelfOrStaff(content.ownerId());
        auditService.log(accessGuard.currentUserId(), "MEDIA_ACCESSED", "symptom_media", id, ipAddress);
        return content;
    }

    public String mediaUrlFor(UUID id) {
        return MediaUrls.forId(id);
    }
}
