package com.medsimple.media.controller;

import com.medsimple.media.dto.MediaUploadResponse;
import com.medsimple.media.service.MediaStorageService;
import com.medsimple.media.storage.MediaContent;
import com.medsimple.media.storage.StoredMedia;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Media upload + retrieval for patient symptom intake (photos, voice notes).
 *
 *   POST /api/v1/media   (multipart, field "file") → { mediaUrl, contentType }
 *   GET  /api/v1/media/{id}                        → the bytes, ownership-gated
 *
 * Both endpoints sit behind the JWT filter (SecurityConfig.anyRequest().authenticated()).
 * The GET is ownership-gated in the service against the media's owner — never world-readable,
 * because media may contain PHI. The bytes themselves are never logged.
 */
@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping(value = "/api/v1/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest httpRequest) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }
        StoredMedia stored = mediaStorageService.upload(bytes, file.getContentType(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MediaUploadResponse(stored.mediaUrl(), stored.contentType()));
    }

    @GetMapping("/api/v1/media/{id}")
    public ResponseEntity<Resource> get(@PathVariable UUID id, HttpServletRequest httpRequest) {
        MediaContent content = mediaStorageService.download(id, httpRequest.getRemoteAddr());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                // Private: media may contain PHI. Do not let any cache hold it.
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(content.resource());
    }
}
