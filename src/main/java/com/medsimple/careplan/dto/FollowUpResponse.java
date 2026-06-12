package com.medsimple.careplan.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FollowUpResponse(
        UUID id,
        UUID initiatedBy,
        boolean system,
        String message,
        String response,
        UUID respondedBy,
        OffsetDateTime respondedAt,
        OffsetDateTime createdAt
) {}
