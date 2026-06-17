package com.medsimple.auth.dto;

import com.medsimple.shared.entity.UserRole;

import java.util.UUID;

/**
 * Client-facing auth payload. The refresh token is intentionally absent — it travels in
 * an httpOnly cookie, never in a body JS can read. Only the short-lived access token is
 * returned here for the SPA to hold in memory.
 */
public record AuthTokens(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        UserRole role
) {
    public static AuthTokens from(AuthResponse r) {
        return new AuthTokens(r.accessToken(), r.tokenType(), r.expiresInSeconds(), r.userId(), r.role());
    }
}
