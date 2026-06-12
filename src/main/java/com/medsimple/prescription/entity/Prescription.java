package com.medsimple.prescription.entity;

import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

// FHIR: MedicationRequest. Authored within a care plan, signed by a physician,
// then delivered electronically (PrescribeIT) or as a PDF fallback.
@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id", nullable = false)
    private CarePlan carePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescribing_physician_id", nullable = false)
    private User prescribingPhysician;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "medication_code")
    private String medicationCode;

    @Column(nullable = false)
    private String dosage;

    @Column(nullable = false)
    private String frequency;

    private String duration;

    @Column(nullable = false)
    @Builder.Default
    private int refills = 0;

    private String instructions;

    // {interactions: [], allergyFlags: [], doseWarnings: []} — populated by the AI safety check.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_safety_checks", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String aiSafetyChecks = "{}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method")
    private DeliveryMethod deliveryMethod;

    @Column(name = "pharmacy_id")
    private UUID pharmacyId;

    // Signed PDF URL in object storage (fallback path).
    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "filled_at")
    private OffsetDateTime filledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
