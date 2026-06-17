package com.medsimple.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the refresh-token cookie. The refresh token lives in an httpOnly cookie so
 * client-side JavaScript (and therefore XSS) cannot read it. Scoped to the auth path
 * so it is only sent to refresh/logout, never to ordinary API calls.
 *
 * {@code secure} is true by default and must stay true in any https environment; it is
 * overridden to false for local http dev (otherwise the browser drops the cookie).
 */
@Component
public class RefreshCookie {

    public static final String NAME = "ms_refresh";
    private static final String PATH = "/api/v1/auth";

    private final boolean secure;
    private final String sameSite;
    private final long ttlSeconds;

    public RefreshCookie(
            @Value("${auth.refresh-cookie.secure:true}") boolean secure,
            @Value("${auth.refresh-cookie.same-site:Strict}") String sameSite,
            @Value("${jwt.refresh-token-ttl-seconds:2592000}") long ttlSeconds) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.ttlSeconds = ttlSeconds;
    }

    /** Cookie carrying a freshly issued refresh token. */
    public ResponseCookie issue(String rawToken) {
        return base(rawToken).maxAge(ttlSeconds).build();
    }

    /** Expired, empty cookie that clears the refresh token from the browser on logout. */
    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH);
    }
}
