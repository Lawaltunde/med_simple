package com.medsimple.diagnostic.dto;

import com.medsimple.diagnostic.entity.RequisitionUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateRequisitionRequest(
        @NotNull UUID episodeId,
        @NotBlank String testType,
        List<String> testCodes,
        String clinicalIndication,
        @NotNull RequisitionUrgency urgency
) {}
