package com.medsimple.auth.security;

import com.medsimple.auth.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

class LoginRateLimiterTest {

    private static final int MAX = 3;
    private static final long LOCKOUT_MIN = 15;

    private LoginRateLimiter limiter(Clock clock) {
        LoginRateLimiter l = new LoginRateLimiter(MAX, LOCKOUT_MIN);
        l.setClock(clock);
        return l;
    }

    @Test
    void allowsUntilThresholdThenLocks() {
        LoginRateLimiter l = limiter(Clock.systemUTC());

        // First MAX-1 failures stay allowed.
        for (int i = 0; i < MAX - 1; i++) {
            l.recordFailure("user@x.com");
            assertThatCode(() -> l.assertAllowed("user@x.com")).doesNotThrowAnyException();
        }

        // The MAX-th failure trips the lock.
        l.recordFailure("user@x.com");
        assertThatThrownBy(() -> l.assertAllowed("user@x.com"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void successClearsTheCounter() {
        LoginRateLimiter l = limiter(Clock.systemUTC());
        l.recordFailure("user@x.com");
        l.recordFailure("user@x.com");

        l.recordSuccess("user@x.com");

        // Counter reset — a fresh failure does not immediately lock.
        l.recordFailure("user@x.com");
        assertThatCode(() -> l.assertAllowed("user@x.com")).doesNotThrowAnyException();
    }

    @Test
    void lockExpiresAfterWindow() {
        Instant t0 = Instant.parse("2026-06-17T12:00:00Z");
        MutableClock clock = new MutableClock(t0);
        LoginRateLimiter l = limiter(clock);

        for (int i = 0; i < MAX; i++) l.recordFailure("user@x.com");
        assertThatThrownBy(() -> l.assertAllowed("user@x.com"))
                .isInstanceOf(TooManyAttemptsException.class);

        // Advance past the lockout window — access is allowed again.
        clock.advance(Duration.ofMinutes(LOCKOUT_MIN).plusSeconds(1));
        assertThatCode(() -> l.assertAllowed("user@x.com")).doesNotThrowAnyException();
    }

    @Test
    void keysAreCaseAndWhitespaceInsensitive() {
        LoginRateLimiter l = limiter(Clock.systemUTC());
        for (int i = 0; i < MAX; i++) l.recordFailure("  User@X.com ");

        // Same identity in different casing/spacing hits the same locked bucket.
        assertThatThrownBy(() -> l.assertAllowed("user@x.com"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void differentIdentifiersAreIsolated() {
        LoginRateLimiter l = limiter(Clock.systemUTC());
        for (int i = 0; i < MAX; i++) l.recordFailure("locked@x.com");

        // A different email is unaffected by another's lockout.
        assertThatCode(() -> l.assertAllowed("other@x.com")).doesNotThrowAnyException();
    }

    // Test-only clock that can be advanced on demand.
    private static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant start) { this.now = start; }
        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }
}
