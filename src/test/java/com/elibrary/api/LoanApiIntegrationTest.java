package com.elibrary.api;

import com.elibrary.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanApiIntegrationTest extends ApiIntegrationTest {

    @Test
    void borrowingReturnsCreatedLoanAndLocationHeader() throws Exception {
        insertMember("member-1", "token-1", 5);
        String bookId = insertBook("Clean Code", 2);

        mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/loans/")))
                .andExpect(jsonPath("$.bookId").value(bookId))
                .andExpect(jsonPath("$.memberId").value("member-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dueAt").exists());

        assertThat(availableLicenses(bookId)).isEqualTo(1);
    }

    @Test
    void borrowingRequiresAuthentication() throws Exception {
        String bookId = insertBook("Clean Code", 1);

        mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void borrowingFailsWhenEveryLicenseIsInUse() throws Exception {
        insertMember("member-1", "token-1", 5);
        insertMember("member-2", "token-2", 5);
        String bookId = insertBook("Single License", 1);
        borrow("token-1", bookId);

        mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId)
                        .header(AUTHORIZATION, bearer("token-2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_UNAVAILABLE"));
    }

    @Test
    void duplicateActiveLoanIsRejected() throws Exception {
        insertMember("member-1", "token-1", 5);
        String bookId = insertBook("Clean Code", 2);
        borrow("token-1", bookId);

        mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOAN"));
    }

    @Test
    void concurrentLoanLimitIsEnforced() throws Exception {
        insertMember("member-1", "token-1", 1);
        String first = insertBook("First", 1);
        String second = insertBook("Second", 1);
        borrow("token-1", first);

        mockMvc.perform(post("/api/v1/books/{bookId}/loans", second)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_LIMIT_REACHED"));
    }

    @Test
    void returningIsIdempotent() throws Exception {
        insertMember("member-1", "token-1", 5);
        String bookId = insertBook("Clean Code", 1);
        String loanId = borrow("token-1", bookId);

        MvcResult first = mockMvc.perform(post("/api/v1/loans/{loanId}/return", loanId)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andReturn();
        String firstReturnedAt = JsonPath.read(first.getResponse().getContentAsString(), "$.returnedAt");

        MvcResult second = mockMvc.perform(post("/api/v1/loans/{loanId}/return", loanId)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isOk())
                .andReturn();
        String secondReturnedAt = JsonPath.read(second.getResponse().getContentAsString(), "$.returnedAt");

        assertThat(secondReturnedAt).isEqualTo(firstReturnedAt);
        assertThat(availableLicenses(bookId)).isEqualTo(1);
    }

    @Test
    void cannotReturnAnotherMembersLoan() throws Exception {
        insertMember("member-1", "token-1", 5);
        insertMember("member-2", "token-2", 5);
        String bookId = insertBook("Clean Code", 1);
        String loanId = borrow("token-1", bookId);

        mockMvc.perform(post("/api/v1/loans/{loanId}/return", loanId)
                        .header(AUTHORIZATION, bearer("token-2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_OWNED"));
    }

    @Test
    void returningUnknownLoanReturns404() throws Exception {
        insertMember("member-1", "token-1", 5);

        mockMvc.perform(post("/api/v1/loans/{loanId}/return", "missing")
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_FOUND"));
    }

    @Test
    void myLoansSupportsStatusFiltersIncludingOverdue() throws Exception {
        insertMember("member-1", "token-1", 5);
        String overdueBook = insertBook("Overdue", 1);
        String returnedBook = insertBook("Returned", 1);
        Clock systemClock = Clock.systemUTC();
        insertLoan("loan-overdue", overdueBook, "member-1",
                systemClock.instant().minus(30, ChronoUnit.DAYS),
                systemClock.instant().minus(16, ChronoUnit.DAYS), null);
        insertLoan("loan-returned", returnedBook, "member-1",
                systemClock.instant().minus(10, ChronoUnit.DAYS),
                systemClock.instant().plus(4, ChronoUnit.DAYS),
                systemClock.instant().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/v1/me/loans")
                        .param("status", "overdue")
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value("loan-overdue"))
                .andExpect(jsonPath("$.items[0].status").value("OVERDUE"));

        mockMvc.perform(get("/api/v1/me/loans")
                        .param("status", "returned")
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].status").value("RETURNED"));

        // "active" means not yet returned, so an overdue loan is still part of it.
        mockMvc.perform(get("/api/v1/me/loans")
                        .param("status", "active")
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].status").value("OVERDUE"));
    }
}
