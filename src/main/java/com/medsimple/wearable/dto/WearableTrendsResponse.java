package com.medsimple.wearable.dto;

import java.util.List;

/**
 * The wearable view for a patient: whether a device is connected (consented + has data), the
 * per-metric trends, and a deterministic, PHI-safe one-line summary for the physician / AI context.
 */
public record WearableTrendsResponse(
        boolean connected,
        List<MetricTrend> metrics,
        String summary
) {}
