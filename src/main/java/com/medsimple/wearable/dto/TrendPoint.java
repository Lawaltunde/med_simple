package com.medsimple.wearable.dto;

import java.time.OffsetDateTime;

// One point in a metric's time series, for the trend chart.
public record TrendPoint(OffsetDateTime at, double value) {}
