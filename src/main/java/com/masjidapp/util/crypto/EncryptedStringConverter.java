package com.masjidapp.util.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Transparently encrypts/decrypts a String column at rest using {@link EncryptionService}.
 * Apply explicitly with {@code @Convert(converter = EncryptedStringConverter.class)} on the
 * sensitive entity field. Not auto-applied, so ordinary String columns are unaffected.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : EncryptionService.getInstance().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EncryptionService.getInstance().decrypt(dbData);
    }
}
