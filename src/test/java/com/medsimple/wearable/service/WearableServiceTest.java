package com.medsimple.wearable.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.patient.entity.ConsentType;
import com.medsimple.patient.entity.PatientConsent;
import com.medsimple.patient.repository.PatientConsentRepository;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.shared.exception.BusinessRuleException;
import com.medsimple.wearable.dao.NormalizedReading;
import com.medsimple.wearable.dao.WearableSyncDao;
import com.medsimple.wearable.dto.IngestReadingRequest;
import com.medsimple.wearable.dto.RecordReadingsRequest;
import com.medsimple.wearable.dto.WearableTrendsResponse;
import com.medsimple.wearable.entity.WearableMetric;
import com.medsimple.wearable.entity.WearableReading;
import com.medsimple.wearable.entity.WearableSource;
import com.medsimple.wearable.repository.WearableReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WearableServiceTest {

    @Mock WearableReadingRepository readingRepository;
    @Mock PatientConsentRepository consentRepository;
    @Mock PatientRepository patientRepository;
    @Mock WearableSyncDao syncDao;
    @Mock AccessGuard accessGuard;
    @Mock AuditService auditService;

    @InjectMocks WearableService service;

    private UUID patientId;
    private User patient;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        patient = User.builder().id(patientId).role(UserRole.PATIENT)
                .firstName("Sam").lastName("Rivera").email("sam@x.com").build();
    }

    private void consented(boolean yes) {
        PatientConsent c = mock(PatientConsent.class);
        lenient().when(c.getRevokedAt()).thenReturn(null);
        when(consentRepository.findFirstByPatientIdAndConsentTypeOrderByCreatedAtDesc(
                patientId, ConsentType.WEARABLE_DATA))
                .thenReturn(yes ? Optional.of(c) : Optional.empty());
    }

    private WearableReading reading(WearableMetric metric, double value, OffsetDateTime at) {
        return WearableReading.builder()
                .id(UUID.randomUUID()).patient(patient).metric(metric).value(value).unit(metric.unit())
                .effectiveAt(at).source(WearableSource.SYNTHETIC).deviceLabel("Synthetic device").build();
    }

    @Test
    void connect_withoutConsent_throwsAndNeverPersists() {
        consented(false);

        assertThatThrownBy(() -> service.connect(patientId, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("WEARABLE_DATA");

        verify(syncDao, never()).sync(any());
        verify(readingRepository, never()).saveAll(any());
    }

    @Test
    void connect_withConsent_syncsAndPersistsWithProvenance() {
        consented(true);
        when(patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)).thenReturn(Optional.of(patient));
        when(syncDao.sync(patientId)).thenReturn(List.of(
                new NormalizedReading(WearableMetric.HEART_RATE, 72, OffsetDateTime.now().minusHours(2))));
        when(syncDao.source()).thenReturn(WearableSource.SYNTHETIC);
        when(syncDao.deviceLabel()).thenReturn("Synthetic device");
        when(readingRepository.findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(eq(patientId), any()))
                .thenReturn(List.of(reading(WearableMetric.HEART_RATE, 72, OffsetDateTime.now().minusHours(2))));

        service.connect(patientId, "1.2.3.4");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WearableReading>> captor = ArgumentCaptor.forClass(List.class);
        verify(readingRepository).saveAll(captor.capture());
        WearableReading saved = captor.getValue().get(0);
        assertThat(saved.getMetric()).isEqualTo(WearableMetric.HEART_RATE);
        assertThat(saved.getUnit()).isEqualTo("bpm");                  // unit derived from the metric
        assertThat(saved.getSource()).isEqualTo(WearableSource.SYNTHETIC); // provenance stamped
        verify(auditService).log(eq(patientId), eq("WEARABLE_CONNECTED"), any(), any(), any(), any());
    }

    @Test
    void ingest_withConsent_persistsAsAggregator() {
        consented(true);
        when(patientRepository.findByIdAndRole(patientId, UserRole.PATIENT)).thenReturn(Optional.of(patient));
        when(readingRepository.findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(eq(patientId), any()))
                .thenReturn(List.of());

        service.ingest(patientId, new RecordReadingsRequest(List.of(
                new IngestReadingRequest(WearableMetric.SPO2, 97, OffsetDateTime.now()))), "1.2.3.4");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WearableReading>> captor = ArgumentCaptor.forClass(List.class);
        verify(readingRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getSource()).isEqualTo(WearableSource.AGGREGATOR);
    }

    @Test
    void ingest_withoutConsent_throws() {
        consented(false);
        assertThatThrownBy(() -> service.ingest(patientId,
                new RecordReadingsRequest(List.of(
                        new IngestReadingRequest(WearableMetric.STEPS, 5000, null))), null))
                .isInstanceOf(BusinessRuleException.class);
        verify(readingRepository, never()).saveAll(any());
    }

    @Test
    void trends_computesPerMetricStatsAndSummary() {
        OffsetDateTime now = OffsetDateTime.now();
        when(readingRepository.findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(eq(patientId), any()))
                .thenReturn(List.of(
                        reading(WearableMetric.HEART_RATE, 100, now.minusHours(1)),  // newest
                        reading(WearableMetric.HEART_RATE, 60, now.minusHours(5)),
                        reading(WearableMetric.STEPS, 6000, now.minusHours(2))));
        lenient().when(consentRepository.findFirstByPatientIdAndConsentTypeOrderByCreatedAtDesc(
                patientId, ConsentType.WEARABLE_DATA)).thenReturn(Optional.empty());

        WearableTrendsResponse res = service.trends(patientId);

        var hr = res.metrics().stream().filter(m -> m.metric() == WearableMetric.HEART_RATE).findFirst().orElseThrow();
        assertThat(hr.latest()).isEqualTo(100);   // newest first
        assertThat(hr.min()).isEqualTo(60);
        assertThat(hr.max()).isEqualTo(100);
        assertThat(hr.count()).isEqualTo(2);
        assertThat(res.summary()).contains("HR 60-100 bpm").contains("consumer-grade");
    }

    @Test
    void trends_noData_summarizesEmpty() {
        when(readingRepository.findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(eq(patientId), any()))
                .thenReturn(List.of());
        lenient().when(consentRepository.findFirstByPatientIdAndConsentTypeOrderByCreatedAtDesc(
                patientId, ConsentType.WEARABLE_DATA)).thenReturn(Optional.empty());

        WearableTrendsResponse res = service.trends(patientId);

        assertThat(res.connected()).isFalse();
        assertThat(res.metrics()).isEmpty();
        assertThat(res.summary()).contains("No wearable data");
    }

    @Test
    void everyEntryPoint_gatesOnAccess() {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("nope"))
                .when(accessGuard).requirePatientSelfOrStaff(patientId);

        assertThatThrownBy(() -> service.trends(patientId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(readingRepository, never()).findByPatientIdAndEffectiveAtAfterOrderByEffectiveAtDesc(any(), any());
    }
}
