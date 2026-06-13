package com.medsimple.shared.converter;

import com.fasterxml.jackson.core.type.TypeReference;

// JSONB <-> List<String>, used for test_codes on diagnostic_requisitions.
public class StringListConverter extends JsonListConverter<String> {

    public StringListConverter() {
        super(new TypeReference<>() {});
    }
}
