package com.elibrary.application.view;

public record BookSummaryView(
        String id,
        String title,
        String author,
        String category,
        int publishedYear,
        int totalLicenses,
        int availableLicenses) {
}
