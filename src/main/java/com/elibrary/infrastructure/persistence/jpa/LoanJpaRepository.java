package com.elibrary.infrastructure.persistence.jpa;

import com.elibrary.domain.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanJpaRepository extends JpaRepository<Loan, String> {

    boolean existsByMemberIdAndBookIdAndReturnedAtIsNull(String memberId, String bookId);

    long countByMemberIdAndReturnedAtIsNull(String memberId);

    Page<Loan> findByMemberId(String memberId, Pageable pageable);

    Page<Loan> findByMemberIdAndReturnedAtIsNull(String memberId, Pageable pageable);

    Page<Loan> findByMemberIdAndReturnedAtIsNotNull(String memberId, Pageable pageable);

    /**
     * Active loans per member are bounded by the membership limit, so overdue filtering can be done
     * in memory without a timestamp predicate (which would need database-specific handling of the
     * ISO-8601 text column).
     */
    List<Loan> findByMemberIdAndReturnedAtIsNullOrderByBorrowedAtDesc(String memberId);
}
