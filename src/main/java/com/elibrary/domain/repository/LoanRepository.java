package com.elibrary.domain.repository;

import com.elibrary.domain.model.Loan;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;

import java.time.Instant;
import java.util.Optional;

public interface LoanRepository {

    Optional<Loan> findById(String id);

    Loan save(Loan loan);

    boolean existsActiveLoan(String memberId, String bookId);

    long countActiveLoans(String memberId);

    PageResult<Loan> findByMember(String memberId, LoanStatusFilter filter, Instant now, PageQuery page);
}
