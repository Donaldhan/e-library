package com.elibrary.domain.repository;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.domain.shared.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookRepository {

    Optional<Book> findById(String id);

    List<Book> findAllById(Collection<String> ids);

    PageResult<Book> search(BookSearchCriteria criteria, BookSort sort, PageQuery page);

    /**
     * Atomically takes one license from the pool.
     *
     * @return {@code true} if a license was taken, {@code false} if none was available
     */
    boolean tryAcquireLicense(String bookId);

    /** Returns one license to the pool. Fails loudly if the pool would exceed its capacity. */
    void releaseLicense(String bookId);

    Book save(Book book);

    long count();
}
