package com.elibrary.application.view;

import com.elibrary.domain.model.ReservationStatus;

import java.time.Instant;

public record ReservationView(
        String id,
        String bookId,
        String bookTitle,
        ReservationStatus status,
        Instant createdAt,
        Instant readyAt,
        Instant expiresAt) {
}
