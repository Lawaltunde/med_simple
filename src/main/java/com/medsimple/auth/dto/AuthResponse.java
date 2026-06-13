package com.medsimple.auth.dto;

import com.medsimple.shared.entity.UserRole;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        UserRole role
) {}
