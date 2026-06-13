package com.medsimple.auth.dto;

import com.medsimple.shared.entity.UserRole;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {}
