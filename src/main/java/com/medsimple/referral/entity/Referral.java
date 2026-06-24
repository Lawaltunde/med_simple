package com.medsimple.referral.entity;

import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// FHIR: ServiceRequest (intent = referral). AI suggests the specialty; the physician
// confirms or overrides it before submission via Ocean eReferral or a PDF fallback.
@Entity
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private com.medsimple.symptom.entity.SymptomEpisode episode;

    // Optional link: a referral can be made before any care plan exists.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id")
    private CarePlan carePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referring_physician_id", nullable = false)
    private User referringPhysician;

    @Column(name = "ai_suggested_specialty", nullable = false)
    private String aiSuggestedSpecialty;

    @Column(name = "confirmed_specialty")
    private String confirmedSpecialty;

    @Column(name = "clinical_summary")
    private String clinicalSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReferralUrgency urgency = ReferralUrgency.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method")
    private ReferralDeliveryMethod deliveryMethod;

    @Column(name = "external_referral_id")
    private String externalReferralId;

    // Referral letter PDF URL in object storage (fallback path).
    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
