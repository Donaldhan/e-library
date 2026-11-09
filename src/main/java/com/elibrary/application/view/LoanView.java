package com.elibrary.application.view;

import com.elibrary.domain.model.LoanStatus;

import java.time.Instant;

public record LoanView(
        String id,
        String bookId,
        String bookTitle,
        String memberId,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        LoanStatus status) {
}
