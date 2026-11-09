package com.elibrary.domain.repository;

/** Search/filter criteria for browsing the catalogue. Blank strings are treated as "no filter". */
public record BookSearchCriteria(String q, String category, boolean availableOnly) {

    public static BookSearchCriteria empty() {
        return new BookSearchCriteria(null, null, false);
    }

    public String normalizedQuery() {
        return q == null || q.isBlank() ? null : q.trim();
    }

    public String normalizedCategory() {
        return category == null || category.isBlank() ? null : category.trim();
    }
}
