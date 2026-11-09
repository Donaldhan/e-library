package com.elibrary.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real application against a throw-away SQLite file per test context, so integration
 * tests exercise the same SQL, constraints and dialect as production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    protected static final String AUTHORIZATION = "Authorization";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected Clock clock;

    @DynamicPropertySource
    static void sqliteDatasource(DynamicPropertyRegistry registry) {
        Path directory = Path.of("target", "test-db");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String path = directory.resolve("elibrary-" + UUID.randomUUID() + ".db").toAbsolutePath().toString();
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + path + "?busy_timeout=5000&journal_mode=WAL&foreign_keys=on");
    }

    @BeforeEach
    void resetDatabase() {
        jdbc.update("DELETE FROM loans");
        jdbc.update("DELETE FROM reservations");
        jdbc.update("DELETE FROM books");
        jdbc.update("DELETE FROM members");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected void insertMember(String id, String token, int maxConcurrentLoans) {
        jdbc.update("""
                        INSERT INTO members (id, name, email, membership_type, max_concurrent_loans, token, created_at)
                        VALUES (?, ?, ?, 'BASIC', ?, ?, ?)
                        """,
                id, id, id + "@example.com", maxConcurrentLoans, token, iso(clock.instant()));
    }

    protected String insertBook(String title, int totalLicenses) {
        return insertBook(title, totalLicenses, "Test Category", 14);
    }

    protected String insertBook(String title, int totalLicenses, String category, int loanPeriodDays) {
        String id = UUID.randomUUID().toString();
        String now = iso(clock.instant());
        jdbc.update("""
                        INSERT INTO books (id, title, author, isbn, category, publisher, published_year, language,
                                           description, cover_url, total_licenses, available_licenses,
                                           loan_period_days, created_at, updated_at)
                        VALUES (?, ?, 'Test Author', ?, ?, 'Test Publisher', 2020, 'en', 'Test description',
                                NULL, ?, ?, ?, ?, ?)
                        """,
                id, title, "isbn-" + id, category, totalLicenses, totalLicenses, loanPeriodDays, now, now);
        return id;
    }

    protected void insertLoan(String id, String bookId, String memberId,
                              Instant borrowedAt, Instant dueAt, Instant returnedAt) {
        jdbc.update("""
                        INSERT INTO loans (id, book_id, member_id, borrowed_at, due_at, returned_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                id, bookId, memberId, iso(borrowedAt), iso(dueAt), returnedAt == null ? null : iso(returnedAt));
    }

    protected void insertReservation(String id, String bookId, String memberId, String status,
                                     Instant createdAt, Instant expiresAt) {
        jdbc.update("""
                        INSERT INTO reservations (id, book_id, member_id, status, created_at, ready_at, expires_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, bookId, memberId, status, iso(createdAt),
                expiresAt == null ? null : iso(createdAt),
                expiresAt == null ? null : iso(expiresAt),
                iso(createdAt));
    }

    protected int availableLicenses(String bookId) {
        Integer value = jdbc.queryForObject("SELECT available_licenses FROM books WHERE id = ?", Integer.class, bookId);
        return value == null ? -1 : value;
    }

    /** Borrows through the real API and returns the created loan id. */
    protected String borrow(String token, String bookId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    protected static String iso(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
