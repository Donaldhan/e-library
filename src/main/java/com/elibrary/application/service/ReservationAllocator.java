package com.elibrary.application.service;

import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Keeps the reservation queue and the license pool in sync.
 *
 * <p>Two deliberate choices:
 * <ul>
 *   <li>A READY hold consumes a license, so nobody can take the copy while the notified member is
 *       collecting it.</li>
 *   <li>Hold expiry is evaluated lazily on the next write/read of reservations instead of by a
 *       scheduled job. That keeps the service free of background infrastructure while still
 *       guaranteeing that a license is never blocked forever; a scheduled sweeper can be added
 *       later without changing the model.</li>
 * </ul>
 */
@Component
public class ReservationAllocator {

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final Duration holdDuration;

    public ReservationAllocator(ReservationRepository reservations, BookRepository books,
                                @Value("${elibrary.reservation.hold-duration:PT48H}") Duration holdDuration) {
        this.reservations = reservations;
        this.books = books;
        this.holdDuration = holdDuration;
    }

    /** Releases licenses held by expired READY reservations and promotes the next member in line. */
    public int expireStaleHolds(Instant now) {
        List<Reservation> expired = reservations.findAllReady().stream()
                .filter(reservation -> reservation.isHoldExpiredAt(now))
                .toList();
        for (Reservation reservation : expired) {
            reservation.expire(now);
            reservations.save(reservation);
            books.releaseLicense(reservation.getBookId());
            promoteWaiting(reservation.getBookId(), now);
        }
        return expired.size();
    }

    /** Returns one license to the pool and immediately hands it to the oldest waiting member. */
    public void releaseAndPromote(String bookId, Instant now) {
        books.releaseLicense(bookId);
        promoteWaiting(bookId, now);
    }

    /**
     * Promotes as many waiting members as there are free licenses, oldest first. Each promotion
     * takes a license via the atomic counter update, so the queue can never over-allocate.
     */
    public void promoteWaiting(String bookId, Instant now) {
        for (Reservation waiting : reservations.findWaitingByBook(bookId)) {
            if (!books.tryAcquireLicense(bookId)) {
                return;
            }
            waiting.markReady(now, holdDuration);
            reservations.save(waiting);
        }
    }
}
