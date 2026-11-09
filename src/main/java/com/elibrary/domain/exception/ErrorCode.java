package com.elibrary.domain.exception;

/**
 * Stable, machine-readable error codes. Clients may branch on these instead of on HTTP status
 * or human-readable messages.
 */
public enum ErrorCode {
    BOOK_NOT_FOUND,
    LOAN_NOT_FOUND,
    RESERVATION_NOT_FOUND,
    BOOK_UNAVAILABLE,
    DUPLICATE_LOAN,
    LOAN_LIMIT_REACHED,
    LOAN_NOT_OWNED,
    RESERVATION_EXISTS,
    BOOK_AVAILABLE,
    RESERVATION_NOT_OWNED,
    RESERVATION_NOT_CANCELLABLE,
    UNAUTHORIZED,
    VALIDATION_FAILED,
    INVALID_REQUEST,
    CONFLICT,
    INTERNAL_ERROR
}
