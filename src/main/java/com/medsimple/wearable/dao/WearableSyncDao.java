package com.medsimple.wearable.dao;

import com.medsimple.wearable.entity.WearableSource;

import java.util.List;
import java.util.UUID;

/**
 * Pulls recent wearable readings for a patient from the device source. The real adapter (a wearable
 * aggregator such as Terra/Validic, Phase 2) and the fallback adapter (synthetic data for
 * development/demo) implement this behind the same port — capability-gated, like every other
 * external integration in the platform.
 */
public interface WearableSyncDao {

    List<NormalizedReading> sync(UUID patientId);

    /** Provenance to stamp on readings from this source. */
    WearableSource source();

    /** Device label to stamp on readings from this source (e.g. "Synthetic device"). */
    String deviceLabel();
}
