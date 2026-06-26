package com.medsimple.wearable.dao;

import com.medsimple.wearable.entity.WearableMetric;
import com.medsimple.wearable.entity.WearableSource;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Default wearable source: generates a realistic recent window of synthetic readings, so the
 * feature is fully testable and demoable with no real device and no aggregator contract. The real
 * aggregator adapter (Terra/Validic) is the Phase 2 seam and would be {@code @Primary} +
 * {@code @ConditionalOnProperty(wearable.adapter=...)}; this fallback is the default otherwise.
 *
 * Deterministic per patient (seeded by the patient id) so the demo data is stable across syncs.
 * Never emits a future-dated reading.
 */
@Component
public class FallbackWearableSyncAdapter implements WearableSyncDao {

    private static final int DAYS = 3;

    @Override
    public List<NormalizedReading> sync(UUID patientId) {
        Random rnd = new Random(patientId.getMostSignificantBits());
        OffsetDateTime now = OffsetDateTime.now();
        List<NormalizedReading> out = new ArrayList<>();

        for (int d = DAYS - 1; d >= 0; d--) {
            OffsetDateTime day = now.minusDays(d);
            add(out, now, WearableMetric.RESTING_HEART_RATE, 56 + rnd.nextInt(15), day, 7);
            for (int h : new int[]{8, 12, 18, 22}) {
                int hr = 62 + rnd.nextInt(28);
                if (h == 18) hr += 12 + rnd.nextInt(18); // an evening exertion bump
                add(out, now, WearableMetric.HEART_RATE, hr, day, h);
            }
            add(out, now, WearableMetric.STEPS, 2500 + rnd.nextInt(6500), day, 21);
            add(out, now, WearableMetric.SLEEP_MINUTES, 360 + rnd.nextInt(120), day, 7);
            add(out, now, WearableMetric.SPO2, 95 + rnd.nextInt(4), day, 9);
            add(out, now, WearableMetric.SPO2, 95 + rnd.nextInt(4), day, 20);
        }
        return out;
    }

    private void add(List<NormalizedReading> out, OffsetDateTime now,
                     WearableMetric metric, double value, OffsetDateTime day, int hour) {
        OffsetDateTime at = day.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (at.isAfter(now)) return; // no future readings
        out.add(new NormalizedReading(metric, value, at));
    }

    @Override
    public WearableSource source() {
        return WearableSource.SYNTHETIC;
    }

    @Override
    public String deviceLabel() {
        return "Synthetic device";
    }
}
