package com.elibrary.application.usecase;

import com.elibrary.application.view.BookSummaryView;
import com.elibrary.application.view.ViewMapper;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.BookSearchCriteria;
import com.elibrary.domain.repository.BookSort;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowseBooksUseCase {

    private final BookRepository books;
    private final ViewMapper mapper;

    public BrowseBooksUseCase(BookRepository books, ViewMapper mapper) {
        this.books = books;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResult<BookSummaryView> execute(BookSearchCriteria criteria, BookSort sort, PageQuery page) {
        return books.search(criteria, sort, page).map(mapper::toSummary);
    }
}
