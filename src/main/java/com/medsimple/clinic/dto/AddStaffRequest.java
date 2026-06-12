package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddStaffRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotNull StaffRole role,
        String licenseNumber,
        String specialization
) {}
