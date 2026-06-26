package com.medsimple.wearable.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.patient.entity.ConsentType;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.PatientConsentRepository;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.shared.exception.BusinessRuleException;
import com.medsimple.wearable.dao.NormalizedReading;
import com.medsimple.wearable.dao.WearableSyncDao;
import com.medsimple.wearable.dto.IngestReadingRequest;
import com.medsimple.wearable.dto.MetricTrend;
import com.medsimple.wearable.dto.RecordReadingsRequest;
import com.medsimple.wearable.dto.TrendPoint;
import com.medsimple.wearable.dto.WearableTrendsResponse;
import com.medsimple.wearable.entity.WearableMetric;
import com.medsimple.wearable.entity.WearableReading;
import com.medsimple.wearable.entity.WearableSource;
import com.medsimple.wearable.repository.WearableReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wearable / device data: connect (sync from the source adapter), ingest (aggregator/webhook),
 * manual entry, and read trends. Consumer-grade, patient-generated, and informational only — the
 * service stores and summarizes the data and never makes an autonomous clinical determination on it.
 *
 * Two gates on every write and read: the caller must be the patient or clinic staff (AccessGuard),
 * and the patient must hold active WEARABLE_DATA consent before anything is ingested.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WearableService {

    private static final int TREND_WINDOW_DAYS = 14;
    private static final int SERIES_MAX_POINTS = 60;

    private final WearableReadingRepository readingRepository;
    private final PatientConsentRepository consentRepository;
    private final PatientRepository patientRepository;
    private final WearableSyncDao syncDao;
    private final AccessGuard accessGuard;
    private final AuditService auditService;

    /** Connect a device: pull recent readings from the source adapter and store them. */
    @Transactional
    public WearableTrendsResponse connect(UUID patientId, String ipAddress) {
        accessGuard.requirePatientSelfOrStaff(patientId);
        requireConsent(patientId);
        User patient = loadPatient(patientId);

        List<NormalizedReading> readings = syncDao.sync(patientId);
        persist(patient, readings, syncDao.source(), syncDao.deviceLabel());

        auditService.log(patientId, "WEARABLE_CONNECTED", "wearable_readings", patientId, ipAddress,
                Map.of("readings", readings.size(), "source", syncDao.source().name()));
        return buildTrends(patientId);
    }

    /** Aggregator / webhook delivery (also the curl-testable ingest path). */
    @Transactional
    public WearableTrendsResponse ingest(UUID patientId, RecordReadingsRequest request, String ipAddress) {
        accessGuard.requirePatientSelfOrStaff(patientId);
        requireConsent(patientId);
        User patient = loadPatient(patientId);

        persist(patient, normalize(request), WearableSource.AGGREGATOR, "Wearable aggregator");
        auditService.log(patientId, "WEARABLE_INGESTED", "wearable_readings", patientId, ipAddress,
                Map.of("readings", request.readings().size(), "source", WearableSource.AGGREGATOR.name()));
        return buildTrends(patientId);
    }

    /** A patient or staff member typed in a reading. */
    @Transactional
    public WearableTrendsResponse recordManual(UUID patientId, RecordReadingsRequest request, String ipAddress) {
        accessGuard.requirePatientSelfOrStaff(patientId);
        requireConsent(patientId);
        User patient = loadPatient(patientId);

        persist(patient, normalize(request), WearableSource.MANUAL, "Manual entry");
        auditService.log(patientId, "WEARABLE_MANUAL_ENTRY", "wearable_readings", patientId, ipAddress,
                Map.of("readings", request.readings().size()));
        return buildTrends(patientId);
    }

    @Transactional(readOnly = true)
    public WearableTrendsResponse trends(UUID patientId) {
        accessGuard.requirePatientSelfOrStaff(patientId);
        return buildTrends(patientId);
    }

    // ── internals ────────────────────────────────────────────────────────────────

    private WearableTrendsResponse buildTrends(UUID patientId) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(TREND_WINDOW_DAYS);
        List<WearableReading> rows =
                readingRepository.findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(patientId, since);

        Map<WearableMetric, List<WearableReading>> byMetric = rows.stream()
                .collect(Collectors.groupingBy(WearableReading::getMetric));

        List<MetricTrend> metrics = new ArrayList<>();
        for (WearableMetric metric : WearableMetric.values()) {
            List<WearableReading> series = byMetric.getOrDefault(metric, List.of());
            if (series.isEmpty()) continue; // only surface metrics we actually have data for
            metrics.add(toTrend(metric, series));
        }

        boolean connected = isConsented(patientId) && !rows.isEmpty();
        return new WearableTrendsResponse(connected, metrics, summarize(metrics));
    }

    private MetricTrend toTrend(WearableMetric metric, List<WearableReading> series) {
        // series is newest-first from the query.
        DoubleSummaryStatistics stats = series.stream().mapToDouble(WearableReading::getValue).summaryStatistics();
        WearableReading latest = series.get(0);
        List<TrendPoint> points = series.stream()
                .limit(SERIES_MAX_POINTS)
                .map(r -> new TrendPoint(r.getEffectiveAt(), r.getValue()))
                .toList();
        return new MetricTrend(metric, metric.label(), metric.unit(),
                latest.getValue(), latest.getEffectiveAt(),
                round(stats.getAverage()), stats.getMin(), stats.getMax(), series.size(), points);
    }

    // Deterministic, PHI-safe one-liner for the physician console / AI context. No identity, just
    // labeled consumer-grade stats — the physician interprets; the platform never concludes.
    private String summarize(List<MetricTrend> metrics) {
        if (metrics.isEmpty()) return "No wearable data on file.";
        String body = metrics.stream().map(m -> switch (m.metric()) {
            case STEPS -> "steps ~" + (long) Math.round(m.average()) + "/day";
            case SLEEP_MINUTES -> "sleep ~" + String.format("%.1f", m.average() / 60.0) + "h";
            case RESTING_HEART_RATE -> "resting HR ~" + (long) Math.round(m.average()) + " bpm";
            case HEART_RATE -> "HR " + (long) Math.round(m.min()) + "-" + (long) Math.round(m.max()) + " bpm";
            case SPO2 -> "SpO2 ~" + (long) Math.round(m.average()) + "%";
        }).collect(Collectors.joining(", "));
        return "Wearable (consumer-grade, last " + TREND_WINDOW_DAYS + "d): " + body + ".";
    }

    private List<NormalizedReading> normalize(RecordReadingsRequest request) {
        return request.readings().stream().map(this::normalize).toList();
    }

    private NormalizedReading normalize(IngestReadingRequest r) {
        OffsetDateTime at = r.effectiveAt() != null ? r.effectiveAt() : OffsetDateTime.now();
        return new NormalizedReading(r.metric(), r.value(), at);
    }

    private void persist(User patient, List<NormalizedReading> readings, WearableSource source, String deviceLabel) {
        List<WearableReading> rows = readings.stream()
                .map(r -> WearableReading.builder()
                        .patient(patient)
                        .metric(r.metric())
                        .value(r.value())
                        .unit(r.metric().unit())
                        .effectiveAt(r.effectiveAt())
                        .source(source)
                        .deviceLabel(deviceLabel)
                        .build())
                .toList();
        readingRepository.saveAll(rows);
    }

    private void requireConsent(UUID patientId) {
        if (!isConsented(patientId)) {
            throw new BusinessRuleException(
                    "Patient has not consented to sharing wearable data (WEARABLE_DATA)");
        }
    }

    private boolean isConsented(UUID patientId) {
        return consentRepository
                .findFirstByPatientIdAndConsentTypeOrderByCreatedAtDesc(patientId, ConsentType.WEARABLE_DATA)
                .map(c -> c.getRevokedAt() == null)
                .orElse(false);
    }

    private User loadPatient(UUID patientId) {
        return patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

    private static Double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
