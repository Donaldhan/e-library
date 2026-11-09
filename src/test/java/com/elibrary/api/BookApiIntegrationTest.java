package com.elibrary.api;

import com.elibrary.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookApiIntegrationTest extends ApiIntegrationTest {

    @Test
    void returnsPageOfCatalogueSortedByTitle() throws Exception {
        insertBook("Gamma", 1);
        insertBook("Alpha", 1);
        insertBook("Beta", 1);

        mockMvc.perform(get("/api/v1/books").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.items[1].title").value("Beta"));
    }

    @Test
    void searchMatchesTitleAuthorAndIsbnAndHonoursCategory() throws Exception {
        insertBook("Clean Code", 1, "Software Engineering", 14);
        insertBook("Clean Architecture", 1, "Software Architecture", 14);
        insertBook("Refactoring", 1, "Software Engineering", 14);

        mockMvc.perform(get("/api/v1/books").param("q", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/books")
                        .param("q", "clean")
                        .param("category", "Software Architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Clean Architecture"));
    }

    @Test
    void availabilityFilterExcludesFullyBorrowedTitles() throws Exception {
        insertMember("member-1", "token-1", 5);
        String borrowed = insertBook("Borrowed", 1);
        insertBook("Free", 1);
        borrow("token-1", borrowed);

        mockMvc.perform(get("/api/v1/books").param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Free"));
    }

    @Test
    void detailIncludesPersonalBorrowingFlagOnlyForAuthenticatedCaller() throws Exception {
        insertMember("member-1", "token-1", 5);
        String bookId = insertBook("Clean Code", 1);
        borrow("token-1", bookId);

        mockMvc.perform(get("/api/v1/books/{bookId}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowedByMe").value(false))
                .andExpect(jsonPath("$.availableLicenses").value(0));

        mockMvc.perform(get("/api/v1/books/{bookId}", bookId)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowedByMe").value(true));
    }

    @Test
    void detailOfUnknownBookReturnsStructured404() throws Exception {
        mockMvc.perform(get("/api/v1/books/{bookId}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "banana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/books").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
