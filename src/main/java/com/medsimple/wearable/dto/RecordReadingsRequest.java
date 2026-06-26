package com.medsimple.wearable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// A batch of readings (manual entry or an aggregator/webhook delivery).
public record RecordReadingsRequest(
        @NotEmpty @Valid List<IngestReadingRequest> readings
) {}
