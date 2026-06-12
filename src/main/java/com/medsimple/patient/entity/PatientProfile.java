package com.medsimple.patient.entity;

import com.medsimple.shared.converter.AllergyListConverter;
import com.medsimple.shared.converter.MedicationListConverter;
import com.medsimple.shared.dto.AllergyDto;
import com.medsimple.shared.dto.MedicationDto;
import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "preferred_pharmacy_id")
    private UUID preferredPharmacyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = AllergyListConverter.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<AllergyDto> allergies = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MedicationListConverter.class)
    @Column(name = "current_medications", columnDefinition = "jsonb")
    @Builder.Default
    private List<MedicationDto> currentMedications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
