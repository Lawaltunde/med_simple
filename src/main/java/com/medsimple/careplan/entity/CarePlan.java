package com.medsimple.careplan.entity;

import com.medsimple.clinic.entity.Clinic;
import com.medsimple.shared.entity.User;
import com.medsimple.symptom.entity.SymptomEpisode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// FHIR: CarePlan — the physician's plan of care for a triaged episode.
@Entity
@Table(name = "care_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private SymptomEpisode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physician_id", nullable = false)
    private User physician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CarePlanStatus status = CarePlanStatus.ACTIVE;

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
