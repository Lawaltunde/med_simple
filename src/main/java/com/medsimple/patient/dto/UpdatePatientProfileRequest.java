package com.medsimple.patient.dto;

import com.medsimple.shared.dto.AllergyDto;
import com.medsimple.shared.dto.MedicationDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdatePatientProfileRequest(
        LocalDate dateOfBirth,
        UUID preferredPharmacyId,
        List<AllergyDto> allergies,
        List<MedicationDto> currentMedications
) {}
