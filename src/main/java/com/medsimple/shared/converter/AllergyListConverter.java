package com.medsimple.shared.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.medsimple.shared.dto.AllergyDto;

import java.util.List;

public class AllergyListConverter extends JsonListConverter<AllergyDto> {

    public AllergyListConverter() {
        super(new TypeReference<>() {});
    }
}
