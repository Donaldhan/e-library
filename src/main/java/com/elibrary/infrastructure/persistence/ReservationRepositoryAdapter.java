package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Reservation;
import com.elibrary.domain.model.ReservationStatus;
import com.elibrary.domain.repository.ReservationRepository;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;
import com.elibrary.infrastructure.persistence.jpa.ReservationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReservationRepositoryAdapter implements ReservationRepository {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final ReservationJpaRepository jpa;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Reservation> findById(String id) {
        return jpa.findById(id);
    }

    @Override
    public Reservation save(Reservation reservation) {
        return jpa.save(reservation);
    }

    @Override
    public Optional<Reservation> findActiveByMemberAndBook(String memberId, String bookId) {
        return jpa.findByMemberIdAndBookIdAndStatusIn(memberId, bookId, ACTIVE_STATUSES);
    }

    @Override
    public List<Reservation> findWaitingByBook(String bookId) {
        return jpa.findByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.WAITING);
    }

    @Override
    public List<Reservation> findAllReady() {
        return jpa.findByStatus(ReservationStatus.READY);
    }

    @Override
    public PageResult<Reservation> findByMember(String memberId, ReservationStatus status, PageQuery page) {
        Pageable pageable = PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Reservation> result = status == null
                ? jpa.findByMemberId(memberId, pageable)
                : jpa.findByMemberIdAndStatus(memberId, status, pageable);
        return new PageResult<>(result.getContent(), page.page(), page.size(), result.getTotalElements());
    }
}
