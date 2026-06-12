package com.medsimple.symptom.entity;

import com.medsimple.clinic.entity.Clinic;
import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// FHIR: EpisodeOfCare — one per patient care complaint.
// Accumulates SymptomSubmissions; holds both AI-assessed and physician-confirmed urgency.
@Entity
@Table(name = "symptom_episodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomEpisode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_clinic_id")
    private Clinic assignedClinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_physician_id")
    private User assignedPhysician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EpisodeStatus status = EpisodeStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_urgency_level")
    private UrgencyLevel aiUrgencyLevel;

    @Column(name = "ai_summary")
    private String aiSummary;

    @Column(name = "ai_analyzed_at")
    private OffsetDateTime aiAnalyzedAt;

    // Physician-confirmed urgency — this is the operative value for the queue.
    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed_urgency_level")
    private UrgencyLevel confirmedUrgencyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "urgency_overridden_by")
    private User urgencyOverriddenBy;

    @Column(name = "urgency_override_reason")
    private String urgencyOverrideReason;

    @Column(name = "urgency_overridden_at")
    private OffsetDateTime urgencyOverriddenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
