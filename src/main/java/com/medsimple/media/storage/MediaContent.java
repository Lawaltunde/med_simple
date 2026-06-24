package com.medsimple.media.storage;

import org.springframework.core.io.Resource;

import java.util.UUID;

/**
 * Loaded media bytes plus their content type and owning user. Returned to the GET endpoint
 * (for display, after the controller gates on {@link #ownerId}) and to the analysis adapters
 * (which read the Resource and hand the bytes to the model). The Resource is a local/in-platform
 * handle — the media never leaves storage as a URL.
 */
public record MediaContent(Resource resource, String contentType, UUID ownerId) {}
