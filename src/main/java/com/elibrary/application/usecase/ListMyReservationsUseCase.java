package com.elibrary.application.usecase;

import com.elibrary.application.service.ReservationAllocator;
import com.elibrary.application.view.ReservationView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.model.ReservationStatus;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.ReservationRepository;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListMyReservationsUseCase {

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final ReservationAllocator allocator;
    private final ViewMapper mapper;
    private final Clock clock;

    public ListMyReservationsUseCase(ReservationRepository reservations, BookRepository books,
                                     ReservationAllocator allocator, ViewMapper mapper, Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.allocator = allocator;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Not read-only on purpose: listing is one of the natural points where expired holds become
     * visible, so the lazy sweep runs here too and keeps the license pool accurate.
     */
    @Transactional
    public PageResult<ReservationView> execute(String memberId, ReservationStatus status, PageQuery page) {
        allocator.expireStaleHolds(clock.instant());

        PageResult<Reservation> result = reservations.findByMember(memberId, status, page);
        Map<String, Book> booksById = books.findAllById(
                        result.items().stream().map(Reservation::getBookId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        return result.map(reservation -> mapper.toReservation(reservation, booksById.get(reservation.getBookId())));
    }
}
