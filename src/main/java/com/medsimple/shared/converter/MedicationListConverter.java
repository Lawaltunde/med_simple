package com.medsimple.shared.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.medsimple.shared.dto.MedicationDto;

import java.util.List;

public class MedicationListConverter extends JsonListConverter<MedicationDto> {

    public MedicationListConverter() {
        super(new TypeReference<>() {});
    }
}
