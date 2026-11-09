package com.elibrary.domain.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Stores {@link Instant} values as ISO-8601 UTC strings.
 *
 * <p>SQLite has no dedicated timestamp type; explicit, fixed-format text keeps the stored value
 * readable and lexicographically sortable while remaining portable to other databases.
 */
@Converter
public class InstantIsoConverter implements AttributeConverter<Instant, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : FORMATTER.format(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Instant.parse(dbData);
    }
}
