package com.medsimple.wearable.entity;

import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One wearable / device reading, mapped to a FHIR Observation (vital-signs / activity).
 * Patient-generated health data: {@link #source} and {@link #deviceLabel} carry provenance so it
 * is always presented as consumer-grade, never as a validated clinical measurement.
 *
 * Informational only — surfaced and summarized for the physician; the platform never makes an
 * autonomous clinical determination on it.
 */
@Entity
@Table(name = "wearable_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WearableReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WearableMetric metric;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false, length = 20)
    private String unit;

    // When the measurement was actually taken (not when we received it).
    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WearableSource source;

    @Column(name = "device_label", length = 80)
    private String deviceLabel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
