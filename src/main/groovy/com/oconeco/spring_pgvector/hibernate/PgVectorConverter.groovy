package com.oconeco.spring_pgvector.hibernate

import groovy.util.logging.Slf4j
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.postgresql.jdbc.PgArray


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Custom Hibernate converter for handling pgvector float[] arrays.
 * This class provides proper conversion between Java float arrays and PostgreSQL vector type.
 */
@Slf4j
@Converter(autoApply = true)
class PgVectorConverter implements AttributeConverter<float[], Object> {

    /*
    @Override
    public String convertToDatabaseColumn(List<Float> attribute) {
        log.warn "convertToDatabaseColumn: $attribute"
        if (attribute == null) return null;
        return "[" + attribute.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    @Override
    public List<Float> convertToEntityAttribute(String dbData) {
        log.warn "ConvertToEntityAttribute: $dbData"
        if (dbData == null || dbData.isEmpty()) return null;
        String trimmed = dbData.replaceAll("[\\[\\]]", ""); // remove brackets
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .map(Float::parseFloat)
                .collect(Collectors.toList());
    }
    */


    @Override
    Object convertToDatabaseColumn(float[] attribute) {
        // Simply return null if the attribute is null
        if (attribute == null) {
            return null
        }
        return attribute
    }

    @Override
    float[] convertToEntityAttribute(Object dbData) {
        // Return null if the database data is null
        if (dbData == null) {
            return null
        }

        try {
            if (dbData instanceof PgArray) {
                PgArray pgArray = (PgArray) dbData
                Object arrayData = pgArray.getArray()

                if (arrayData instanceof Float[]) {
                    Float[] boxedArray = (Float[]) arrayData
                    float[] result = new float[boxedArray.length]
                    for (int i = 0; i < boxedArray.length; i++) {
                        result[i] = boxedArray[i] != null ? boxedArray[i] : 0.0f
                    }
                    return result
                } else if (arrayData instanceof float[]) {
                    return (float[]) arrayData
                } else if (arrayData instanceof Object[]) {
                    Object[] objArray = (Object[]) arrayData
                    float[] result = new float[objArray.length]
                    for (int i = 0; i < objArray.length; i++) {
                        result[i] = objArray[i] != null ? ((Number) objArray[i]).floatValue() : 0.0f
                    }
                    return result
                }
            } else if (dbData instanceof float[]) {
                return (float[]) dbData
            }

            log.warn("Unexpected data type for vector: {}", dbData.getClass().getName())
            return null
        } catch (Exception e) {
            log.error("Error converting vector data from database", e)
            return null
        }
    }
}
