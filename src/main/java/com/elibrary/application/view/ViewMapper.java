package com.elibrary.application.view;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.model.Reservation;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Maps domain entities to API-facing views, including derived status computed from the clock. */
@Component
public class ViewMapper {

    private final Clock clock;

    public ViewMapper(Clock clock) {
        this.clock = clock;
    }

    public BookSummaryView toSummary(Book book) {
        return new BookSummaryView(book.getId(), book.getTitle(), book.getAuthor(), book.getCategory(),
                book.getPublishedYear(), book.getTotalLicenses(), book.getAvailableLicenses());
    }

    public BookDetailView toDetail(Book book, boolean borrowedByMe) {
        return new BookDetailView(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getCategory(), book.getPublisher(), book.getPublishedYear(), book.getLanguage(),
                book.getDescription(), book.getCoverUrl(), book.getTotalLicenses(),
                book.getAvailableLicenses(), book.getLoanPeriodDays(), borrowedByMe);
    }

    public LoanView toLoan(Loan loan, Book book) {
        return new LoanView(loan.getId(), loan.getBookId(), book == null ? null : book.getTitle(),
                loan.getMemberId(), loan.getBorrowedAt(), loan.getDueAt(), loan.getReturnedAt(),
                loan.statusAt(clock.instant()));
    }

    public ReservationView toReservation(Reservation reservation, Book book) {
        return new ReservationView(reservation.getId(), reservation.getBookId(),
                book == null ? null : book.getTitle(), reservation.getStatus(),
                reservation.getCreatedAt(), reservation.getReadyAt(), reservation.getExpiresAt());
    }
}
