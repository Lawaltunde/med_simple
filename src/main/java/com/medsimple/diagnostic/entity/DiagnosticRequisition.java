package com.medsimple.diagnostic.entity;

import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.shared.converter.StringListConverter;
import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// FHIR: ServiceRequest. Physician orders labs/imaging; delivered electronically
// or as a printable PDF requisition (fallback).
@Entity
@Table(name = "diagnostic_requisitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticRequisition {

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
    @JoinColumn(name = "ordering_physician_id", nullable = false)
    private User orderingPhysician;

    @Column(name = "test_type", nullable = false)
    private String testType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = StringListConverter.class)
    @Column(name = "test_codes", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> testCodes = new ArrayList<>();

    @Column(name = "clinical_indication")
    private String clinicalIndication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequisitionUrgency urgency = RequisitionUrgency.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequisitionStatus status = RequisitionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method")
    private DiagnosticDeliveryMethod deliveryMethod;

    @Column(name = "diagnostic_centre_id")
    private UUID diagnosticCentreId;

    // Printable requisition PDF URL in object storage (fallback path).
    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "appointment_at")
    private OffsetDateTime appointmentAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "booked_at")
    private OffsetDateTime bookedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
