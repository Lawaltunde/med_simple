package com.medsimple.notification.dto;

import com.medsimple.notification.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

// In-app notification shape returned to the client feed.
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
