package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.BookSearchCriteria;
import com.elibrary.domain.repository.BookSort;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;
import com.elibrary.infrastructure.persistence.jpa.BookJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class BookRepositoryAdapter implements BookRepository {

    private final BookJpaRepository jpa;
    private final Clock clock;

    public BookRepositoryAdapter(BookJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Optional<Book> findById(String id) {
        return jpa.findById(id);
    }

    @Override
    public List<Book> findAllById(Collection<String> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
    }

    @Override
    public PageResult<Book> search(BookSearchCriteria criteria, BookSort sort, PageQuery page) {
        Pageable pageable = PageRequest.of(page.page(), page.size(), sortOf(sort));
        Page<Book> result = jpa.search(criteria.normalizedQuery(), criteria.normalizedCategory(),
                criteria.availableOnly(), pageable);
        return new PageResult<>(result.getContent(), page.page(), page.size(), result.getTotalElements());
    }

    @Override
    public boolean tryAcquireLicense(String bookId) {
        return jpa.acquireLicense(bookId, nowIso()) > 0;
    }

    @Override
    public void releaseLicense(String bookId) {
        if (jpa.releaseLicense(bookId, nowIso()) == 0) {
            throw new IllegalStateException("License counter for book " + bookId + " is inconsistent");
        }
    }

    @Override
    public Book save(Book book) {
        return jpa.save(book);
    }

    @Override
    public long count() {
        return jpa.count();
    }

    private String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.format(clock.instant());
    }

    private static Sort sortOf(BookSort sort) {
        return switch (sort) {
            case TITLE -> Sort.by(Sort.Direction.ASC, "title");
            case PUBLISHED_YEAR -> Sort.by(Sort.Direction.DESC, "publishedYear");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
