package com.medsimple.symptom.dto;

import java.util.UUID;

public record StartEpisodeRequest(
        UUID clinicId
) {}
