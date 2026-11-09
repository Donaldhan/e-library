package com.elibrary.application.usecase;

import com.elibrary.application.service.ReservationAllocator;
import com.elibrary.application.view.LoanView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.exception.ForbiddenException;
import com.elibrary.domain.exception.NotFoundException;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnLoanUseCase {

    private final LoanRepository loans;
    private final BookRepository books;
    private final ReservationAllocator allocator;
    private final ViewMapper mapper;
    private final Clock clock;

    public ReturnLoanUseCase(LoanRepository loans, BookRepository books,
                             ReservationAllocator allocator, ViewMapper mapper, Clock clock) {
        this.loans = loans;
        this.books = books;
        this.allocator = allocator;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Returning is intentionally idempotent: repeating the call returns the original loan instead of
     * failing, which makes client retries after a timeout safe. Ownership is still enforced first so
     * a member cannot probe or release someone else's loan.
     */
    @Transactional
    public LoanView execute(String memberId, String loanId) {
        Instant now = clock.instant();
        Loan loan = loans.findById(loanId).orElseThrow(() -> NotFoundException.loan(loanId));
        if (!loan.isOwnedBy(memberId)) {
            throw new ForbiddenException(ErrorCode.LOAN_NOT_OWNED, "Loan " + loanId + " belongs to another member");
        }
        Book book = books.findById(loan.getBookId()).orElse(null);

        if (loan.isActive()) {
            loan.markReturned(now);
            loans.save(loan);
            // Hand the freed license straight to the next member in the queue, if any.
            allocator.releaseAndPromote(loan.getBookId(), now);
        }
        return mapper.toLoan(loan, book);
    }
}
