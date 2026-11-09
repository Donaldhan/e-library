package com.elibrary.application.usecase;

import com.elibrary.application.service.ReservationAllocator;
import com.elibrary.application.view.LoanView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.exception.NotFoundException;
import com.elibrary.domain.exception.UnauthorizedException;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.model.Member;
import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.model.ReservationStatus;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import com.elibrary.domain.repository.MemberRepository;
import com.elibrary.domain.repository.ReservationRepository;
import com.elibrary.domain.service.LendingPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class BorrowBookUseCase {

    private final BookRepository books;
    private final LoanRepository loans;
    private final MemberRepository members;
    private final ReservationRepository reservations;
    private final ReservationAllocator allocator;
    private final ViewMapper mapper;
    private final Clock clock;

    public BorrowBookUseCase(BookRepository books, LoanRepository loans, MemberRepository members,
                             ReservationRepository reservations, ReservationAllocator allocator,
                             ViewMapper mapper, Clock clock) {
        this.books = books;
        this.loans = loans;
        this.members = members;
        this.reservations = reservations;
        this.allocator = allocator;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public LoanView execute(String memberId, String bookId) {
        Instant now = clock.instant();
        allocator.expireStaleHolds(now);

        Member member = members.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated member no longer exists"));
        Book book = books.findById(bookId).orElseThrow(() -> NotFoundException.book(bookId));

        // Cheap, deterministic checks first so callers get a precise error before any license moves.
        LendingPolicy.ensureMayBorrow(member, loans.countActiveLoans(memberId),
                loans.existsActiveLoan(memberId, bookId));

        Reservation activeReservation = reservations.findActiveByMemberAndBook(memberId, bookId).orElse(null);
        if (activeReservation != null && activeReservation.getStatus() == ReservationStatus.READY) {
            // The license was already reserved for this member when the hold was promoted.
            activeReservation.fulfill(now);
            reservations.save(activeReservation);
        } else {
            boolean acquired = books.tryAcquireLicense(bookId);
            if (!acquired) {
                throw new ConflictException(ErrorCode.BOOK_UNAVAILABLE,
                        "All licenses for book " + bookId + " are currently in use");
            }
            if (activeReservation != null) {
                // Member was waiting, but a license happened to free up first: fulfil their queue entry.
                activeReservation.fulfill(now);
                reservations.save(activeReservation);
            }
        }

        Loan loan = Loan.create(UUID.randomUUID().toString(), bookId, memberId, now, book.getLoanPeriodDays());
        loans.save(loan);
        return mapper.toLoan(loan, book);
    }
}
