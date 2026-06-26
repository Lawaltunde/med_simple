package com.medsimple.wearable.dto;

import com.medsimple.wearable.entity.WearableMetric;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * One reading to ingest — used by manual entry and by the aggregator/webhook (curl-testable) path.
 * The unit is derived from the metric on the server, so callers only send the value. effectiveAt
 * defaults to "now" when omitted.
 */
public record IngestReadingRequest(
        @NotNull WearableMetric metric,
        double value,
        OffsetDateTime effectiveAt
) {}
