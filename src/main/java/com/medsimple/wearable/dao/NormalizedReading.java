package com.medsimple.wearable.dao;

import com.medsimple.wearable.entity.WearableMetric;

import java.time.OffsetDateTime;

// A reading normalized to the platform's metric + unit, ready to persist. The unit is implied by
// the metric, so it is not carried here.
public record NormalizedReading(WearableMetric metric, double value, OffsetDateTime effectiveAt) {}
