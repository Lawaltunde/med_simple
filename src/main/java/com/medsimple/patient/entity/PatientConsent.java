package com.medsimple.patient.entity;

import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// Immutable append-only record per PHIPA. No @UpdateTimestamp — rows are never modified.
@Entity
@Table(name = "patient_consents")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(nullable = false)
    private String version;

    @Column(name = "granted_at")
    private OffsetDateTime grantedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
