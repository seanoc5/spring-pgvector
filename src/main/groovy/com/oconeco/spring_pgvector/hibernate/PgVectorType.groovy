package com.oconeco.spring_pgvector.hibernate

import groovy.util.logging.Slf4j
import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * Custom Hibernate type for handling pgvector float[] arrays.
 * This class provides proper conversion between Java float arrays and PostgreSQL vector type.
 */
@Slf4j
class PgVectorType implements UserType<float[]> {

    int[] sqlTypes() {
        return [Types.ARRAY] as int[]
    }
    
    @Override
    int getSqlType() {
        return Types.ARRAY
    }

    @Override
    Class<float[]> returnedClass() {
        return float[].class
    }

    @Override
    boolean equals(float[] x, float[] y) {
        if (x == null || y == null) {
            return x == y
        }
        if (x.length != y.length) {
            return false
        }
        for (int i = 0; i < x.length; i++) {
            if (x[i] != y[i]) {
                return false
            }
        }
        return true
    }

    @Override
    int hashCode(float[] x) {
        return x != null ? Arrays.hashCode(x) : 0
    }

    @Override
    float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        if (rs.getObject(position) == null) {
            return null
        }
        
        try {
            // Get the array from the result set
            java.sql.Array array = rs.getArray(position)
            if (array == null) {
                return null
            }
            
            // Convert to float array
            Object arrayData = array.getArray()
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
            
            log.warn("Unexpected array type: {}", arrayData.getClass().getName())
            return null
        } catch (Exception e) {
            log.error("Error getting vector data", e)
            throw new HibernateException("Error getting vector data", e)
        }
    }

    @Override
    void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) throws SQLException {
        try {
            if (value == null) {
                // Use setObject with null instead of setNull for PostgreSQL vector type
                st.setObject(index, null);
                return;
            }
            
            // Create a PostgreSQL float array
            Connection connection = st.getConnection();
            java.sql.Array array = connection.createArrayOf("float4", toObjectArray(value));
            st.setArray(index, array);
        } catch (Exception e) {
            log.error("Error setting vector data", e);
            throw new HibernateException("Error setting vector data", e);
        }
    }

    private Float[] toObjectArray(float[] primitiveArray) {
        Float[] objectArray = new Float[primitiveArray.length]
        for (int i = 0; i < primitiveArray.length; i++) {
            objectArray[i] = primitiveArray[i]
        }
        return objectArray
    }

    @Override
    float[] deepCopy(float[] value) {
        if (value == null) {
            return null
        }
        float[] copy = new float[value.length]
        System.arraycopy(value, 0, copy, 0, value.length)
        return copy
    }

    @Override
    boolean isMutable() {
        return true
    }

    @Override
    Serializable disassemble(float[] value) {
        return (Serializable) deepCopy(value)
    }

    @Override
    float[] assemble(Serializable cached, Object owner) {
        return deepCopy((float[]) cached)
    }

    @Override
    float[] replace(float[] original, float[] target, Object owner) {
        return deepCopy(original)
    }
}
