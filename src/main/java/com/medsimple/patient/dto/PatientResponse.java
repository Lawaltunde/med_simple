package com.medsimple.patient.dto;

import com.medsimple.shared.dto.AllergyDto;
import com.medsimple.shared.dto.MedicationDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean active,
        boolean emailVerified,
        OffsetDateTime createdAt,
        Profile profile
) {
    public record Profile(
            LocalDate dateOfBirth,
            UUID preferredPharmacyId,
            List<AllergyDto> allergies,
            List<MedicationDto> currentMedications
    ) {}
}
