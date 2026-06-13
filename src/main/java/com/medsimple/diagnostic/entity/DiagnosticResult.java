package com.medsimple.diagnostic.entity;

import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

// FHIR: DiagnosticReport.
// PHIPA gate: releasedToPatientAt stays NULL until the physician releases the result.
// The patient cannot see a result until that timestamp is set — enforced in DiagnosticService.
@Entity
@Table(name = "diagnostic_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    private DiagnosticRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_physician_id", nullable = false)
    private User orderingPhysician;

    @Column(name = "raw_result_text")
    private String rawResultText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_result_data", columnDefinition = "jsonb")
    private String rawResultData;

    @Column(name = "ai_summary")
    private String aiSummary;

    @Column(name = "physician_interpretation")
    private String physicianInterpretation;

    // PHIPA visibility gate — NULL means the patient cannot see this result yet.
    @Column(name = "released_to_patient_at")
    private OffsetDateTime releasedToPatientAt;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
