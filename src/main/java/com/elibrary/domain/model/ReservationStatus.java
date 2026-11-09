package com.elibrary.domain.model;

import com.elibrary.domain.exception.InvalidRequestException;

import java.util.Locale;

public enum ReservationStatus {
    WAITING,
    READY,
    FULFILLED,
    CANCELLED,
    EXPIRED;

    public boolean isActive() {
        return this == WAITING || this == READY;
    }

    /** Returns {@code null} for a blank filter, so the repository can mean "any status". */
    public static ReservationStatus parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unsupported reservation status '" + raw
                    + "'. Allowed values: waiting, ready, fulfilled, cancelled, expired");
        }
    }
}
