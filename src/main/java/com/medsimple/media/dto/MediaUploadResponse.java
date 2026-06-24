package com.medsimple.media.dto;

/**
 * The result of a media upload: the retrievable in-platform mediaUrl the client attaches
 * to a symptom submission, plus the resolved content type. No filesystem path is exposed.
 */
public record MediaUploadResponse(String mediaUrl, String contentType) {}
