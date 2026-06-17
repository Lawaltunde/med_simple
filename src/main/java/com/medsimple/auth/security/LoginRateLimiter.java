package com.medsimple.auth.security;

import com.medsimple.auth.exception.TooManyAttemptsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-identifier login throttle. After {@code maxAttempts} consecutive failures an
 * identifier (email) is locked for {@code lockoutMinutes}; a success clears the counter.
 *
 * In-memory and per-instance by design — adequate for a single-node deployment. A
 * multi-node deployment would back this with a shared store (Redis); the contract
 * ({@link #assertAllowed}, {@link #recordFailure}, {@link #recordSuccess}) stays the same.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration lockout;
    private Clock clock = Clock.systemUTC();

    private final ConcurrentHashMap<String, Attempts> byKey = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${auth.login.max-attempts:5}") int maxAttempts,
            @Value("${auth.login.lockout-minutes:15}") long lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
    }

    /** Throws if the identifier is currently locked out. Call before checking credentials. */
    public void assertAllowed(String key) {
        Attempts a = byKey.get(normalize(key));
        if (a != null && a.lockedUntil != null && Instant.now(clock).isBefore(a.lockedUntil)) {
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again after " + lockout.toMinutes() + " minutes.");
        }
    }

    /** Record a failed attempt; trip the lock once the threshold is crossed. */
    public void recordFailure(String key) {
        byKey.compute(normalize(key), (k, a) -> {
            Instant now = Instant.now(clock);
            if (a == null || (a.lockedUntil != null && !now.isBefore(a.lockedUntil))) {
                a = new Attempts(); // fresh window, or the previous lock has expired
            }
            a.count++;
            if (a.count >= maxAttempts) {
                a.lockedUntil = now.plus(lockout);
            }
            return a;
        });
    }

    /** Clear all state for an identifier after a successful login. */
    public void recordSuccess(String key) {
        byKey.remove(normalize(key));
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    // Visible for tests: drive the lock window deterministically.
    void setClock(Clock clock) {
        this.clock = clock;
    }

    private static final class Attempts {
        private int count;
        private Instant lockedUntil;
    }
}
