package com.elibrary.domain.repository;

import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.model.ReservationStatus;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    Optional<Reservation> findById(String id);

    Reservation save(Reservation reservation);

    /** Active means status WAITING or READY. */
    Optional<Reservation> findActiveByMemberAndBook(String memberId, String bookId);

    /** WAITING reservations for a book, oldest first. */
    List<Reservation> findWaitingByBook(String bookId);

    /** All READY reservations; used for lazy hold expiry, bounded by the number of titles. */
    List<Reservation> findAllReady();

    PageResult<Reservation> findByMember(String memberId, ReservationStatus status, PageQuery page);
}
