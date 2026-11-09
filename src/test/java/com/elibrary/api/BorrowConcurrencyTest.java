package com.elibrary.api;

import com.elibrary.application.usecase.BorrowBookUseCase;
import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scenario that usually breaks naive implementations: N members race for the last license.
 * Exactly one must win, and the database must not end up with more active loans than licenses.
 */
class BorrowConcurrencyTest extends ApiIntegrationTest {

    private static final int CONTENDERS = 8;

    @Autowired
    private BorrowBookUseCase borrowBook;

    @Test
    void onlyOneBorrowerReceivesTheLastLicense() throws Exception {
        String bookId = insertBook("Single License", 1);
        for (int i = 0; i < CONTENDERS; i++) {
            insertMember("member-" + i, "token-" + i, 5);
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONTENDERS);
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ErrorCode>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < CONTENDERS; i++) {
                String memberId = "member-" + i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        borrowBook.execute(memberId, bookId);
                        return null;
                    } catch (ConflictException ex) {
                        return ex.code();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<ErrorCode> results = new ArrayList<>();
            for (Future<ErrorCode> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(results).filteredOn(Objects::isNull).hasSize(1);
            assertThat(results).filteredOn(Objects::nonNull).containsOnly(ErrorCode.BOOK_UNAVAILABLE);
        } finally {
            executor.shutdownNow();
        }

        assertThat(availableLicenses(bookId)).isZero();
        Integer activeLoans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loans WHERE book_id = ? AND returned_at IS NULL", Integer.class, bookId);
        assertThat(activeLoans).isEqualTo(1);
    }
}
