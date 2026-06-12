package com.medsimple.clinic.dto;

import com.medsimple.clinic.entity.ClinicType;

public record UpdateClinicRequest(
        String name,
        ClinicType type,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String postalCode,
        String phone,
        String fax,
        String email
) {}
