package com.elibrary.api;

import com.elibrary.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationApiIntegrationTest extends ApiIntegrationTest {

    @Test
    void membersCanJoinTheWaitingListWhenNoLicenseIsFree() throws Exception {
        insertMember("member-1", "token-1", 5);
        insertMember("member-2", "token-2", 5);
        String bookId = insertBook("Single License", 1);
        borrow("token-1", bookId);

        mockMvc.perform(post("/api/v1/books/{bookId}/reservations", bookId)
                        .header(AUTHORIZATION, bearer("token-2")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/reservations/")))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }

    @Test
    void reservingAnAvailableBookIsRejected() throws Exception {
        insertMember("member-1", "token-1", 5);
        String bookId = insertBook("Available", 1);

        mockMvc.perform(post("/api/v1/books/{bookId}/reservations", bookId)
                        .header(AUTHORIZATION, bearer("token-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_AVAILABLE"));
    }

    @Test
    void duplicateReservationIsRejected() throws Exception {
        insertMember("member-1", "token-1", 5);
        insertMember("member-2", "token-2", 5);
        String bookId = insertBook("Single License", 1);
        borrow("token-1", bookId);
        reserve("token-2", bookId);

        mockMvc.perform(post("/api/v1/books/{bookId}/reservations", bookId)
                        .header(AUTHORIZATION, bearer("token-2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_EXISTS"));
    }

    @Test
    void returningPromotesTheOldestWaiterAndHoldsALicense() throws Exception {
        insertMember("member-a", "token-a", 5);
        insertMember("member-b", "token-b", 5);
        insertMember("member-c", "token-c", 5);
        String bookId = insertBook("Single License", 1);

        String loanId = borrow("token-a", bookId);
        String bobReservation = reserve("token-b", bookId);
        reserve("token-c", bookId);

        mockMvc.perform(post("/api/v1/loans/{loanId}/return", loanId)
                        .header(AUTHORIZATION, bearer("token-a")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/reservations").header(AUTHORIZATION, bearer("token-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(bobReservation))
                .andExpect(jsonPath("$.items[0].status").value("READY"))
                .andExpect(jsonPath("$.items[0].expiresAt").exists());

        // The READY hold consumes the only license: nobody else may take it.
        assertThat(availableLicenses(bookId)).isZero();
        mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId)
                        .header(AUTHORIZATION, bearer("token-c")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_UNAVAILABLE"));

        // The holder can convert the hold into a loan.
        mockMvc.perform(post("/api/v1/books/{bookId}/loans", bookId)
                        .header(AUTHORIZATION, bearer("token-b")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/reservations")
                        .param("status", "fulfilled")
                        .header(AUTHORIZATION, bearer("token-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void cancellingAReadyHoldReleasesTheLicense() throws Exception {
        insertMember("member-a", "token-a", 5);
        insertMember("member-b", "token-b", 5);
        String bookId = insertBook("Single License", 1);

        String loanId = borrow("token-a", bookId);
        String reservationId = reserve("token-b", bookId);
        mockMvc.perform(post("/api/v1/loans/{loanId}/return", loanId)
                        .header(AUTHORIZATION, bearer("token-a")))
                .andExpect(status().isOk());
        assertThat(availableLicenses(bookId)).isZero();

        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", reservationId)
                        .header(AUTHORIZATION, bearer("token-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(availableLicenses(bookId)).isEqualTo(1);
    }

    @Test
    void expiredHoldIsReleasedLazilyOnRead() throws Exception {
        insertMember("member-b", "token-b", 5);
        String bookId = insertBook("Single License", 1);
        jdbc.update("UPDATE books SET available_licenses = 0 WHERE id = ?", bookId);
        String reservationId = UUID.randomUUID().toString();
        insertReservation(reservationId, bookId, "member-b", "READY",
                clock.instant().minus(3, ChronoUnit.HOURS),
                clock.instant().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/me/reservations").header(AUTHORIZATION, bearer("token-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(reservationId))
                .andExpect(jsonPath("$.items[0].status").value("EXPIRED"));

        assertThat(availableLicenses(bookId)).isEqualTo(1);
    }

    private String reserve(String token, String bookId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books/{bookId}/reservations", bookId)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
