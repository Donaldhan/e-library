package com.elibrary.application.usecase;

import com.elibrary.application.service.ReservationAllocator;
import com.elibrary.application.view.ReservationView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.exception.ForbiddenException;
import com.elibrary.domain.exception.NotFoundException;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.model.ReservationStatus;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class CancelReservationUseCase {

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final ReservationAllocator allocator;
    private final ViewMapper mapper;
    private final Clock clock;

    public CancelReservationUseCase(ReservationRepository reservations, BookRepository books,
                                    ReservationAllocator allocator, ViewMapper mapper, Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.allocator = allocator;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public ReservationView execute(String memberId, String reservationId) {
        Instant now = clock.instant();
        allocator.expireStaleHolds(now);

        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> NotFoundException.reservation(reservationId));
        if (!reservation.getMemberId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.RESERVATION_NOT_OWNED,
                    "Reservation " + reservationId + " belongs to another member");
        }
        if (!reservation.getStatus().isActive()) {
            throw new ConflictException(ErrorCode.RESERVATION_NOT_CANCELLABLE,
                    "Reservation " + reservationId + " is " + reservation.getStatus() + " and cannot be cancelled");
        }

        boolean licenseWasHeld = reservation.getStatus() == ReservationStatus.READY;
        reservation.cancel(now);
        reservations.save(reservation);
        if (licenseWasHeld) {
            allocator.releaseAndPromote(reservation.getBookId(), now);
        }
        Book book = books.findById(reservation.getBookId()).orElse(null);
        return mapper.toReservation(reservation, book);
    }
}
