package com.elibrary.domain.model;

import com.elibrary.domain.model.converter.InstantIsoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A lending transaction. Status is always derived from {@code dueAt} / {@code returnedAt}
 * instead of being persisted, so there is no scheduled job that can leave rows in a stale state.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "book_id", nullable = false, updatable = false)
    private String bookId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private String memberId;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private Instant borrowedAt;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "returned_at")
    private Instant returnedAt;

    protected Loan() {
        // for JPA
    }

    private Loan(String id, String bookId, String memberId, Instant borrowedAt, Instant dueAt) {
        this.id = requireText(id, "id");
        this.bookId = requireText(bookId, "bookId");
        this.memberId = requireText(memberId, "memberId");
        this.borrowedAt = Objects.requireNonNull(borrowedAt, "borrowedAt");
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt");
    }

    public static Loan create(String id, String bookId, String memberId, Instant now, int loanPeriodDays) {
        Objects.requireNonNull(now, "now");
        if (loanPeriodDays <= 0) {
            throw new IllegalArgumentException("loanPeriodDays must be positive");
        }
        return new Loan(id, bookId, memberId, now, now.plus(loanPeriodDays, ChronoUnit.DAYS));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /** Marks the loan as returned. Repeated calls are rejected; idempotency is handled by the use case. */
    public void markReturned(Instant now) {
        if (returnedAt != null) {
            throw new IllegalStateException("loan " + id + " is already returned");
        }
        if (now.isBefore(borrowedAt)) {
            throw new IllegalArgumentException("return time cannot precede borrow time");
        }
        this.returnedAt = now;
    }

    public boolean isActive() {
        return returnedAt == null;
    }

    public boolean isOverdue(Instant now) {
        return isActive() && dueAt.isBefore(now);
    }

    public LoanStatus statusAt(Instant now) {
        if (returnedAt != null) {
            return LoanStatus.RETURNED;
        }
        return isOverdue(now) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
    }

    public boolean isOwnedBy(String memberId) {
        return this.memberId.equals(memberId);
    }

    public String getId() {
        return id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getMemberId() {
        return memberId;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }
}
