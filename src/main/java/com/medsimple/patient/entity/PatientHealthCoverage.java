package com.medsimple.patient.entity;

import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_health_coverage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientHealthCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_type", nullable = false)
    private CoverageType coverageType;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    // AES-256-GCM encrypted at application layer. Never return plaintext in responses.
    @Column(name = "card_number_enc", nullable = false)
    private String cardNumberEnc;

    private String province;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
