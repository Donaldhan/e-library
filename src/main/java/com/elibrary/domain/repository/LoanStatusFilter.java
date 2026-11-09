package com.elibrary.domain.repository;

import com.elibrary.domain.exception.InvalidRequestException;

import java.util.Locale;

public enum LoanStatusFilter {
    ALL,
    ACTIVE,
    OVERDUE,
    RETURNED;

    public static LoanStatusFilter from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(
                    "Unsupported status '" + raw + "'. Allowed values: all, active, overdue, returned");
        }
    }
}
