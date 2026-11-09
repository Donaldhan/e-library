package com.elibrary.application.view;

public record BookDetailView(
        String id,
        String title,
        String author,
        String isbn,
        String category,
        String publisher,
        int publishedYear,
        String language,
        String description,
        String coverUrl,
        int totalLicenses,
        int availableLicenses,
        int loanPeriodDays,
        boolean borrowedByMe) {
}
