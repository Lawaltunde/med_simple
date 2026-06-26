package com.medsimple.media.dao;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Bounded retry with backoff for the external media-model calls (Whisper transcription,
 * GPT-4o vision). Those calls fail intermittently on otherwise-valid input — a momentary
 * timeout, rate-limit, or 5xx — and succeed on an immediate retry.
 *
 * <p>Without this, a single transient blip became permanent: the adapter wrote a
 * "transcription failed" placeholder, the submission was marked COMPLETED, and a COMPLETED
 * submission is never reprocessed — so the clinician console showed a failure note for a
 * recording that would have transcribed fine on the next attempt.
 *
 * <p>Deterministic, no external dependency. PHI: never logs the call's input or output — only
 * the operation label and the exception type/message (model errors carry HTTP status, not media).
 */
@Slf4j
final class MediaModelRetry {

    private MediaModelRetry() {}

    /**
     * Invoke {@code call} up to {@code maxAttempts} times, sleeping {@code baseBackoffMillis}
     * doubled each retry (e.g. 250ms, 500ms) between attempts. Returns the first success.
     * Re-throws the last failure if every attempt fails — the caller decides the fallback.
     */
    static <T> T withRetry(String operation, int maxAttempts, long baseBackoffMillis, Supplier<T> call) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                last = e;
                if (attempt < maxAttempts) {
                    long backoff = baseBackoffMillis << (attempt - 1);
                    log.warn("{} failed (attempt {}/{}), retrying in {}ms: {}",
                            operation, attempt, maxAttempts, backoff, e.toString());
                    sleep(backoff);
                } else {
                    log.error("{} failed after {} attempt(s)", operation, maxAttempts, e);
                }
            }
        }
        throw last;
    }

    private static void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during media-model retry backoff", ie);
        }
    }
}
