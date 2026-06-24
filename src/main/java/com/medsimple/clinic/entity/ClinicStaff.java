package com.medsimple.clinic.entity;

import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "clinic_staff",
        uniqueConstraints = @UniqueConstraint(columnNames = {"clinic_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole role;

    @Column(name = "license_number")
    private String licenseNumber;

    private String specialization;

    // ── Canadian College credentials (physicians) ───────────────────────────
    /** Provincial College code, e.g. CPSO (Ontario), CMQ (Quebec), CPSBC (BC), CPSA (Alberta). */
    @Column(name = "college")
    private String college;

    /** Registration / licence number issued by the College. */
    @Column(name = "registration_number")
    private String registrationNumber;

    /** Province of licensure (2-letter), e.g. ON. */
    @Column(name = "license_province")
    private String licenseProvince;

    /** Provincial billing number (e.g. OHIP billing number). */
    @Column(name = "billing_number")
    private String billingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    /** How the credential was verified — e.g. CPSO (registry API) or MANUAL_REVIEW. */
    @Column(name = "verification_source")
    private String verificationSource;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

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
