package com.medsimple.careplan.dto;

import com.medsimple.careplan.entity.CarePlanStatus;

public record UpdateCarePlanRequest(
        CarePlanStatus status,
        String notes
) {}
