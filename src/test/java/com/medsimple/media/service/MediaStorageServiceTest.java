package com.medsimple.media.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.media.exception.MediaTooLargeException;
import com.medsimple.media.exception.UnsupportedMediaTypeException;
import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.MediaStorageDao;
import com.medsimple.media.storage.StoredMedia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaStorageServiceTest {

    @Mock MediaStorageDao storage;
    @Mock AuditService auditService;
    @Mock AccessGuard accessGuard;

    private MediaStorageService service;
    private final long maxBytes = 1000;

    @BeforeEach
    void setUp() {
        service = new MediaStorageService(storage, auditService, accessGuard, maxBytes);
    }

    // ── upload validation ─────────────────────────────────────────────────────────

    @Test
    void upload_validImage_storesAndAuditsMetadataOnly() {
        UUID uploader = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        when(accessGuard.currentUserId()).thenReturn(uploader);
        when(storage.store(any(), eq("image/jpeg"), eq(uploader)))
                .thenReturn(new StoredMedia(mediaId, "/api/v1/media/" + mediaId, "image/jpeg", uploader));

        StoredMedia result = service.upload(new byte[]{1, 2, 3}, "image/jpeg", "1.2.3.4");

        assertThat(result.mediaUrl()).isEqualTo("/api/v1/media/" + mediaId);
        // Audit carries uploader + type + size; never the bytes.
        verify(auditService).log(uploader, "MEDIA_UPLOADED", "symptom_media", mediaId, "1.2.3.4", "image/jpeg;3b");
    }

    @Test
    void upload_validAudio_isAccepted() {
        UUID uploader = UUID.randomUUID();
        when(accessGuard.currentUserId()).thenReturn(uploader);
        when(storage.store(any(), eq("audio/webm"), eq(uploader)))
                .thenReturn(new StoredMedia(UUID.randomUUID(), "/api/v1/media/x", "audio/webm", uploader));

        service.upload(new byte[]{9}, "audio/webm", "ip");

        verify(storage).store(any(), eq("audio/webm"), eq(uploader));
    }

    @Test
    void upload_disallowedType_rejected() {
        assertThatThrownBy(() -> service.upload(new byte[]{1}, "application/pdf", "ip"))
                .isInstanceOf(UnsupportedMediaTypeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void upload_emptyBody_rejected() {
        assertThatThrownBy(() -> service.upload(new byte[]{}, "image/png", "ip"))
                .isInstanceOf(UnsupportedMediaTypeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void upload_overSizeLimit_rejected() {
        byte[] big = new byte[(int) maxBytes + 1];
        assertThatThrownBy(() -> service.upload(big, "image/png", "ip"))
                .isInstanceOf(MediaTooLargeException.class);
        verifyNoInteractions(storage);
    }

    // ── download ownership gate ─────────────────────────────────────────────────────

    @Test
    void download_gatesOnOwnerThenReturnsContentAndAudits() {
        UUID mediaId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID reader = UUID.randomUUID();
        MediaContent content = new MediaContent(new ByteArrayResource(new byte[]{7}), "image/png", owner);
        when(storage.load(mediaId)).thenReturn(content);
        when(accessGuard.currentUserId()).thenReturn(reader);

        MediaContent result = service.download(mediaId, "1.1.1.1");

        assertThat(result).isSameAs(content);
        verify(accessGuard).requirePatientSelfOrStaff(owner);
        verify(auditService).log(reader, "MEDIA_ACCESSED", "symptom_media", mediaId, "1.1.1.1");
    }

    @Test
    void download_otherPatientsMedia_denied() {
        UUID mediaId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        MediaContent content = new MediaContent(new ByteArrayResource(new byte[]{1}), "image/png", owner);
        when(storage.load(mediaId)).thenReturn(content);
        doThrow(new AccessDeniedException("nope")).when(accessGuard).requirePatientSelfOrStaff(owner);

        assertThatThrownBy(() -> service.download(mediaId, "ip"))
                .isInstanceOf(AccessDeniedException.class);
        // The access gate runs before any audit-access entry is written.
        verify(auditService, never()).log(any(), eq("MEDIA_ACCESSED"), any(), any(), any());
    }
}
