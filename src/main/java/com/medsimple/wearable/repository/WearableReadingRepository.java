package com.medsimple.wearable.repository;

import com.medsimple.wearable.entity.WearableMetric;
import com.medsimple.wearable.entity.WearableReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WearableReadingRepository extends JpaRepository<WearableReading, UUID> {

    // Trend series for one metric over a window, newest first.
    List<WearableReading> findByPatientIdAndMetricAndEffectiveAtAfterOrderByEffectiveAtDesc(
            UUID patientId, WearableMetric metric, OffsetDateTime since);

    // Everything for a patient since a cutoff (used to build the multi-metric trend view).
    List<WearableReading> findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(
            UUID patientId, OffsetDateTime since);

    boolean existsByPatientId(UUID patientId);
}
