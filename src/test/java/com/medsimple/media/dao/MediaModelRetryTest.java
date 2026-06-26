package com.medsimple.media.dao;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaModelRetryTest {

    // 0ms backoff keeps the gate test fast and deterministic.
    private static final long NO_BACKOFF = 0L;

    @Test
    void returnsImmediately_whenFirstAttemptSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        String result = MediaModelRetry.withRetry("op", 3, NO_BACKOFF, () -> {
            calls.incrementAndGet();
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesAndSucceeds_whenAnEarlyAttemptThrows() {
        AtomicInteger calls = new AtomicInteger();
        String result = MediaModelRetry.withRetry("op", 3, NO_BACKOFF, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new RuntimeException("transient 503");
            }
            return "recovered";
        });
        assertThat(result).isEqualTo("recovered");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void rethrowsLastFailure_afterExhaustingAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() ->
                MediaModelRetry.withRetry("op", 3, NO_BACKOFF, () -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("attempt " + calls.get());
                }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("attempt 3");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void singleAttempt_doesNotRetry() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() ->
                MediaModelRetry.withRetry("op", 1, NO_BACKOFF, () -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("boom");
                }))
                .isInstanceOf(RuntimeException.class);
        assertThat(calls.get()).isEqualTo(1);
    }
}
