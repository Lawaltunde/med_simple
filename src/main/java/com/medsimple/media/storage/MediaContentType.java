package com.medsimple.media.storage;

import com.medsimple.symptom.entity.InputType;

import java.util.Map;
import java.util.Set;

/**
 * Allow-list of media content types the platform accepts, mapped to the symptom
 * {@link InputType} they belong to. Validation lives here (deterministic, same input →
 * same answer) rather than in latent space or scattered across controllers.
 *
 * Anything not in this map is rejected at upload — patients cannot store arbitrary files.
 */
public final class MediaContentType {

    private MediaContentType() {}

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> AUDIO_TYPES =
            Set.of("audio/webm", "audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/m4a", "audio/wav", "audio/x-wav");

    // File extension used on disk per accepted content type — keeps stored files inspectable.
    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/webp", "webp"),
            Map.entry("audio/webm", "webm"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/mp4", "m4a"),
            Map.entry("audio/x-m4a", "m4a"),
            Map.entry("audio/m4a", "m4a"),
            Map.entry("audio/wav", "wav"),
            Map.entry("audio/x-wav", "wav"));

    public static boolean isImage(String contentType) {
        return contentType != null && IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    public static boolean isAudio(String contentType) {
        return contentType != null && AUDIO_TYPES.contains(contentType.toLowerCase());
    }

    public static boolean isAllowed(String contentType) {
        return isImage(contentType) || isAudio(contentType);
    }

    /** The InputType a content type maps to, or null when the type is not accepted. */
    public static InputType inputTypeFor(String contentType) {
        if (isImage(contentType)) return InputType.PHOTO;
        if (isAudio(contentType)) return InputType.VOICE;
        return null;
    }

    public static String extensionFor(String contentType) {
        return EXTENSIONS.getOrDefault(contentType == null ? "" : contentType.toLowerCase(), "bin");
    }
}
