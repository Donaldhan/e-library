package com.elibrary.application.usecase;

import com.elibrary.application.service.ReservationAllocator;
import com.elibrary.application.view.ReservationView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.exception.NotFoundException;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import com.elibrary.domain.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreateReservationUseCase {

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final LoanRepository loans;
    private final ReservationAllocator allocator;
    private final ViewMapper mapper;
    private final Clock clock;

    public CreateReservationUseCase(ReservationRepository reservations, BookRepository books,
                                    LoanRepository loans, ReservationAllocator allocator,
                                    ViewMapper mapper, Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.loans = loans;
        this.allocator = allocator;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Reservations only make sense when every license is in use; if a license is free the member
     * should simply borrow. Rejecting the request keeps the queue meaningful and avoids the
     * ambiguity of "reserved but immediately available".
     */
    @Transactional
    public ReservationView execute(String memberId, String bookId) {
        Instant now = clock.instant();
        allocator.expireStaleHolds(now);

        Book book = books.findById(bookId).orElseThrow(() -> NotFoundException.book(bookId));
        if (loans.existsActiveLoan(memberId, bookId)) {
            throw new ConflictException(ErrorCode.DUPLICATE_LOAN,
                    "Member already has an active loan for book " + bookId);
        }
        if (reservations.findActiveByMemberAndBook(memberId, bookId).isPresent()) {
            throw new ConflictException(ErrorCode.RESERVATION_EXISTS,
                    "Member already has an active reservation for book " + bookId);
        }
        if (book.hasAvailableLicense()) {
            throw new ConflictException(ErrorCode.BOOK_AVAILABLE,
                    "Book " + bookId + " is available; borrow it directly instead of reserving");
        }

        Reservation reservation = Reservation.waiting(UUID.randomUUID().toString(), bookId, memberId, now);
        reservations.save(reservation);
        return mapper.toReservation(reservation, book);
    }
}
