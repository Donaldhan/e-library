package com.elibrary.domain.model;

import com.elibrary.domain.model.converter.InstantIsoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A place in the waiting queue for a title whose licenses are all in use.
 *
 * <p>State machine:
 * <pre>
 *   WAITING --promote--> READY --borrow--> FULFILLED
 *      |                   |--hold expires--> EXPIRED
 *      |                   |--cancel--------> CANCELLED
 *      +--cancel---------------------------> CANCELLED
 * </pre>
 * A READY reservation holds one license, which is why promotion decrements (and expiry/cancel
 * returns) the book's available-license counter.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "book_id", nullable = false, updatable = false)
    private String bookId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "ready_at")
    private Instant readyAt;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
        // for JPA
    }

    private Reservation(String id, String bookId, String memberId, Instant now) {
        this.id = requireText(id, "id");
        this.bookId = requireText(bookId, "bookId");
        this.memberId = requireText(memberId, "memberId");
        this.status = ReservationStatus.WAITING;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public static Reservation waiting(String id, String bookId, String memberId, Instant now) {
        return new Reservation(id, bookId, memberId, now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public void markReady(Instant now, Duration holdDuration) {
        if (status != ReservationStatus.WAITING) {
            throw new IllegalStateException("only WAITING reservations can be promoted, was " + status);
        }
        this.status = ReservationStatus.READY;
        this.readyAt = now;
        this.expiresAt = now.plus(holdDuration);
        this.updatedAt = now;
    }

    public void fulfill(Instant now) {
        if (status != ReservationStatus.READY) {
            throw new IllegalStateException("only READY reservations can be fulfilled, was " + status);
        }
        this.status = ReservationStatus.FULFILLED;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        if (!status.isActive()) {
            throw new IllegalStateException("only active reservations can be cancelled, was " + status);
        }
        this.status = ReservationStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void expire(Instant now) {
        if (status != ReservationStatus.READY) {
            throw new IllegalStateException("only READY reservations can expire, was " + status);
        }
        this.status = ReservationStatus.EXPIRED;
        this.updatedAt = now;
    }

    /** True when a READY hold is past its collection deadline; such holds must be released. */
    public boolean isHoldExpiredAt(Instant now) {
        return status == ReservationStatus.READY && expiresAt != null && !expiresAt.isAfter(now);
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

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
