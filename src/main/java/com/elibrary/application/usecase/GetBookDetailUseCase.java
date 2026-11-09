package com.elibrary.application.usecase;

import com.elibrary.application.view.BookDetailView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.exception.NotFoundException;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetBookDetailUseCase {

    private final BookRepository books;
    private final LoanRepository loans;
    private final ViewMapper mapper;

    public GetBookDetailUseCase(BookRepository books, LoanRepository loans, ViewMapper mapper) {
        this.books = books;
        this.loans = loans;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BookDetailView execute(String bookId, Optional<String> viewerId) {
        Book book = books.findById(bookId).orElseThrow(() -> NotFoundException.book(bookId));
        boolean borrowedByMe = viewerId
                .map(memberId -> loans.existsActiveLoan(memberId, bookId))
                .orElse(false);
        return mapper.toDetail(book, borrowedByMe);
    }
}
