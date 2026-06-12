package com.medsimple.patient.entity;

import com.medsimple.clinic.entity.Clinic;
import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "patient_clinic_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "clinic_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientClinicRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    // Null for walk-in patients until an episode is assigned to a physician.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_physician_id")
    private User assignedPhysician;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
