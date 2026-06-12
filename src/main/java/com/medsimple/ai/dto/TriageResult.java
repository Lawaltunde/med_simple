package com.medsimple.ai.dto;

import com.medsimple.symptom.entity.UrgencyLevel;

public record TriageResult(
        UrgencyLevel urgencyLevel,
        String summary
) {}
