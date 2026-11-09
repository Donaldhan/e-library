package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Loan;
import com.elibrary.domain.repository.LoanRepository;
import com.elibrary.domain.repository.LoanStatusFilter;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;
import com.elibrary.infrastructure.persistence.jpa.LoanJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class LoanRepositoryAdapter implements LoanRepository {

    private final LoanJpaRepository jpa;

    public LoanRepositoryAdapter(LoanJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Loan> findById(String id) {
        return jpa.findById(id);
    }

    @Override
    public Loan save(Loan loan) {
        return jpa.save(loan);
    }

    @Override
    public boolean existsActiveLoan(String memberId, String bookId) {
        return jpa.existsByMemberIdAndBookIdAndReturnedAtIsNull(memberId, bookId);
    }

    @Override
    public long countActiveLoans(String memberId) {
        return jpa.countByMemberIdAndReturnedAtIsNull(memberId);
    }

    @Override
    public PageResult<Loan> findByMember(String memberId, LoanStatusFilter filter, Instant now, PageQuery page) {
        Pageable pageable = PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "borrowedAt"));
        return switch (filter) {
            case ALL -> fromPage(jpa.findByMemberId(memberId, pageable), page);
            case ACTIVE -> fromPage(jpa.findByMemberIdAndReturnedAtIsNull(memberId, pageable), page);
            case RETURNED -> fromPage(jpa.findByMemberIdAndReturnedAtIsNotNull(memberId, pageable), page);
            case OVERDUE -> overdue(memberId, now, page);
        };
    }

    private PageResult<Loan> overdue(String memberId, Instant now, PageQuery page) {
        List<Loan> overdue = jpa.findByMemberIdAndReturnedAtIsNullOrderByBorrowedAtDesc(memberId).stream()
                .filter(loan -> loan.isOverdue(now))
                .toList();
        int from = Math.min(page.page() * page.size(), overdue.size());
        int to = Math.min(from + page.size(), overdue.size());
        return new PageResult<>(overdue.subList(from, to), page.page(), page.size(), overdue.size());
    }

    private static PageResult<Loan> fromPage(Page<Loan> result, PageQuery page) {
        return new PageResult<>(result.getContent(), page.page(), page.size(), result.getTotalElements());
    }
}
