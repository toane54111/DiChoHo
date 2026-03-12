package com.gomarket.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Hibernate Converter: float[] ↔ pgvector string format
 *
 * pgvector lưu vector dưới dạng string: "[0.1,0.2,0.3,...]"
 * Converter này chuyển đổi giữa Java float[] và String để Hibernate
 * có thể đọc/ghi cột embedding kiểu vector(768)
 */
@Converter(autoApply = false)
public class VectorTypeConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(attribute[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;

        // Remove brackets: "[0.1,0.2,...]" -> "0.1,0.2,..."
        String clean = dbData.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) return null;

        String[] parts = clean.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }
}
