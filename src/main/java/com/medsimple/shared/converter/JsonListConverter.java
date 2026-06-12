package com.medsimple.shared.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;

import java.util.ArrayList;
import java.util.List;

public abstract class JsonListConverter<T> implements AttributeConverter<List<T>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final TypeReference<List<T>> typeReference;

    protected JsonListConverter(TypeReference<List<T>> typeReference) {
        this.typeReference = typeReference;
    }

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize list to JSON", e);
        }
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(dbData, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize JSON to list", e);
        }
    }
}
