package com.elibrary.domain.model;

import com.elibrary.domain.model.converter.InstantIsoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A licensable digital title.
 *
 * <p>{@code totalLicenses} is the number of concurrent readers the library may serve.
 * {@code availableLicenses} is a counter of licenses that are neither on loan nor held for a
 * waiting member. It is mutated only through atomic conditional updates in the persistence
 * adapter, so the invariant {@code available + onLoan + held == total} cannot be violated by two
 * concurrent borrow requests.
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author", nullable = false)
    private String author;

    @Column(name = "isbn", nullable = false)
    private String isbn;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "publisher", nullable = false)
    private String publisher;

    @Column(name = "published_year", nullable = false)
    private int publishedYear;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "total_licenses", nullable = false)
    private int totalLicenses;

    @Column(name = "available_licenses", nullable = false)
    private int availableLicenses;

    @Column(name = "loan_period_days", nullable = false)
    private int loanPeriodDays;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Book() {
        // for JPA
    }

    private Book(String id, String title, String author, String isbn, String category, String publisher,
                 int publishedYear, String language, String description, String coverUrl,
                 int totalLicenses, int loanPeriodDays, Instant now) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.author = requireText(author, "author");
        this.isbn = requireText(isbn, "isbn");
        this.category = requireText(category, "category");
        this.publisher = requireText(publisher, "publisher");
        this.language = requireText(language, "language");
        this.description = description == null ? "" : description;
        this.coverUrl = coverUrl;
        this.publishedYear = publishedYear;
        if (totalLicenses < 0) {
            throw new IllegalArgumentException("totalLicenses must not be negative");
        }
        if (loanPeriodDays <= 0) {
            throw new IllegalArgumentException("loanPeriodDays must be positive");
        }
        this.totalLicenses = totalLicenses;
        this.availableLicenses = totalLicenses;
        this.loanPeriodDays = loanPeriodDays;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public static Book create(String id, String title, String author, String isbn, String category,
                              String publisher, int publishedYear, String language, String description,
                              String coverUrl, int totalLicenses, int loanPeriodDays, Instant now) {
        return new Book(id, title, author, isbn, category, publisher, publishedYear, language,
                description, coverUrl, totalLicenses, loanPeriodDays, now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public boolean hasAvailableLicense() {
        return availableLicenses > 0;
    }

    public Duration loanPeriod() {
        return Duration.ofDays(loanPeriodDays);
    }

    /** Read-only view of the license counter; mutations go through the repository's atomic update. */
    public int getAvailableLicenses() {
        return availableLicenses;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getCategory() {
        return category;
    }

    public String getPublisher() {
        return publisher;
    }

    public int getPublishedYear() {
        return publishedYear;
    }

    public String getLanguage() {
        return language;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getTotalLicenses() {
        return totalLicenses;
    }

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
