package com.oconeco.spring_pgvector.hibernate

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import org.postgresql.util.PGobject

import java.io.Serializable
import java.sql.*
import java.util.Arrays
import java.util.Objects

class PgVectorType implements UserType<float[]> {

    @Override
    int getSqlType() {
        return Types.OTHER
    }

    @Override
    Class<float[]> returnedClass() {
        return float[].class
    }

    @Override
    boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y)
    }

    @Override
    int hashCode(float[] x) {
        return Arrays.hashCode(x)
    }

    @Override
    float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String value = rs.getString(position)
        if (value == null) return null

        value = value.replaceAll("[\\[\\]]", "")
        String[] parts = value.split(",")
        float[] vector = new float[parts.length]
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim())
        }
        return vector
    }

    @Override
    void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER)
            return
        }

        PGobject pgObject = new PGobject()
        pgObject.setType("vector")
        pgObject.setValue(Arrays.toString(value).replaceAll("\\s+", "")) // e.g., [0.1,0.2,0.3]
        st.setObject(index, pgObject)
    }

    @Override
    float[] deepCopy(float[] value) {
        return value == null ? null : Arrays.copyOf(value, value.length)
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
