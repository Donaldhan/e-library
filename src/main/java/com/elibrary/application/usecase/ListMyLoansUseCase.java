package com.elibrary.application.usecase;

import com.elibrary.application.view.LoanView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import com.elibrary.domain.repository.LoanStatusFilter;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListMyLoansUseCase {

    private final LoanRepository loans;
    private final BookRepository books;
    private final ViewMapper mapper;
    private final Clock clock;

    public ListMyLoansUseCase(LoanRepository loans, BookRepository books, ViewMapper mapper, Clock clock) {
        this.loans = loans;
        this.books = books;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult<LoanView> execute(String memberId, LoanStatusFilter filter, PageQuery page) {
        PageResult<Loan> result = loans.findByMember(memberId, filter, clock.instant(), page);
        // Batch-load titles for the page to avoid an N+1 lookup per loan.
        Map<String, Book> booksById = books.findAllById(
                        result.items().stream().map(Loan::getBookId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        return result.map(loan -> mapper.toLoan(loan, booksById.get(loan.getBookId())));
    }
}
