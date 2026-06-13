package com.medsimple.diagnostic.dto;

import jakarta.validation.constraints.NotBlank;

// Posted when a result comes back from the lab (by lab integration or staff entry).
public record RecordResultRequest(
        @NotBlank String rawResultText,
        String rawResultData
) {}
