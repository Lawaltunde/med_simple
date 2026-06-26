package com.medsimple.wearable.dto;

import com.medsimple.wearable.entity.WearableMetric;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Summary + recent series for one metric over the trend window. latest/average/min/max are null
 * when there are no readings. The series is newest-first and capped for the chart.
 */
public record MetricTrend(
        WearableMetric metric,
        String label,
        String unit,
        Double latest,
        OffsetDateTime latestAt,
        Double average,
        Double min,
        Double max,
        int count,
        List<TrendPoint> series
) {}
