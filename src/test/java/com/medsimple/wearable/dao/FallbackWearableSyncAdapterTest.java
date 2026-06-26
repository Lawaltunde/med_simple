package com.medsimple.wearable.dao;

import com.medsimple.wearable.entity.WearableMetric;
import com.medsimple.wearable.entity.WearableSource;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackWearableSyncAdapterTest {

    private final FallbackWearableSyncAdapter adapter = new FallbackWearableSyncAdapter();

    @Test
    void sync_producesReadingsForEveryMetric() {
        List<NormalizedReading> readings = adapter.sync(UUID.randomUUID());

        assertThat(readings).isNotEmpty();
        assertThat(readings.stream().map(NormalizedReading::metric).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(WearableMetric.values());
    }

    @Test
    void sync_neverEmitsAFutureReading() {
        OffsetDateTime now = OffsetDateTime.now();
        assertThat(adapter.sync(UUID.randomUUID()))
                .allSatisfy(r -> assertThat(r.effectiveAt()).isBeforeOrEqualTo(now.plusSeconds(1)));
    }

    @Test
    void sync_isDeterministicPerPatient() {
        UUID patient = UUID.randomUUID();
        List<String> a = signature(adapter.sync(patient));
        List<String> b = signature(adapter.sync(patient));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void sync_stampsSyntheticProvenance() {
        assertThat(adapter.source()).isEqualTo(WearableSource.SYNTHETIC);
        assertThat(adapter.deviceLabel()).isNotBlank();
    }

    // metric+value, ignoring exact timestamps, to compare two syncs of the same patient.
    private List<String> signature(List<NormalizedReading> readings) {
        return readings.stream().map(r -> r.metric() + ":" + r.value()).toList();
    }
}
