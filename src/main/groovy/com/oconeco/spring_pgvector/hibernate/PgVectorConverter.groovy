package com.oconeco.spring_pgvector.hibernate

import groovy.util.logging.Slf4j
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.postgresql.util.PGobject

import java.sql.Array
import java.sql.Connection
import java.sql.SQLException

/**
 * Custom Hibernate converter for handling pgvector float[] arrays.
 * This class provides proper conversion between Java float arrays and PostgreSQL vector type.
 */
@Slf4j
@Converter(autoApply = false)
class PgVectorConverter implements AttributeConverter<float[], Object> {
    
    @Override
    Object convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            log.debug("Converting null to database column")
            return null
        }
        
        try {
            // Create a PGobject with type 'vector'
            PGobject pgObject = new PGobject()
            pgObject.setType("vector")
            
            // Convert float[] to a properly formatted string for pgvector
            // Format: [0.1,0.2,0.3]
            StringBuilder sb = new StringBuilder()
            sb.append('[')
            
            for (int i = 0; i < attribute.length; i++) {
                if (i > 0) {
                    sb.append(',')
                }
                // Use simple string representation without scientific notation
                sb.append(String.format(Locale.US, "%.10f", attribute[i]))
            }
            
            sb.append(']')
            String vectorStr = sb.toString()
            
            log.debug("Converting to database column. Vector length: {}, First few values: {}", 
                attribute.length, 
                attribute.length > 5 ? Arrays.toString(Arrays.copyOf(attribute, 5)) + "..." : Arrays.toString(attribute))
            log.debug("Final vector string: {}", vectorStr.length() > 100 ? vectorStr.substring(0, 100) + "..." : vectorStr)
            
            pgObject.setValue(vectorStr)
            return pgObject
        } catch (Exception e) {
            log.error("Error converting float[] to PGobject: {}", e.getMessage(), e)
            throw new RuntimeException("Error converting vector data: " + e.getMessage(), e)
        }
    }

    @Override
    float[] convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            log.debug("Converting null from database column")
            return null
        }
        
        try {
            String vectorStr
            
            if (dbData instanceof PGobject) {
                vectorStr = ((PGobject) dbData).getValue()
                log.debug("Converting from PGobject: {}", 
                    vectorStr != null && vectorStr.length() > 50 ? vectorStr.substring(0, 50) + "..." : vectorStr)
            } else if (dbData instanceof String) {
                vectorStr = (String) dbData
                log.debug("Converting from String: {}", 
                    vectorStr.length() > 50 ? vectorStr.substring(0, 50) + "..." : vectorStr)
            } else {
                log.warn("Unexpected data type for vector: {}", dbData.getClass().getName())
                return null
            }
            
            if (vectorStr == null || vectorStr.isBlank()) {
                return null
            }
            
            // Remove brackets and split by comma
            vectorStr = vectorStr.replaceAll("[\\[\\]]", "")
            String[] parts = vectorStr.split(",")
            
            // Convert string parts to float array
            float[] result = new float[parts.length]
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim())
            }
            
            log.debug("Converted to float array of length: {}", result.length)
            return result
        } catch (Exception e) {
            log.error("Error converting database data to float[]: {}", e.getMessage(), e)
            throw new RuntimeException("Error converting vector data: " + e.getMessage(), e)
        }
    }
}
