package com.elibrary.infrastructure.persistence.jpa;

import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<Reservation, String> {

    Optional<Reservation> findByMemberIdAndBookIdAndStatusIn(String memberId, String bookId,
                                                             Collection<ReservationStatus> statuses);

    List<Reservation> findByBookIdAndStatusOrderByCreatedAtAsc(String bookId, ReservationStatus status);

    List<Reservation> findByStatus(ReservationStatus status);

    Page<Reservation> findByMemberId(String memberId, Pageable pageable);

    Page<Reservation> findByMemberIdAndStatus(String memberId, ReservationStatus status, Pageable pageable);
}
