package com.elibrary.domain.repository;

import com.elibrary.domain.exception.InvalidRequestException;

import java.util.Locale;

public enum BookSort {
    TITLE,
    PUBLISHED_YEAR,
    NEWEST;

    public static BookSort from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TITLE;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "title" -> TITLE;
            case "publishedyear", "published_year", "published-year", "year" -> PUBLISHED_YEAR;
            case "newest", "createdat", "created_at" -> NEWEST;
            default -> throw new InvalidRequestException(
                    "Unsupported sort '" + raw + "'. Allowed values: title, publishedYear, newest");
        };
    }
}
