package com.elibrary.interfaces.http;

import com.elibrary.application.usecase.CancelReservationUseCase;
import com.elibrary.application.usecase.CreateReservationUseCase;
import com.elibrary.application.usecase.ListMyReservationsUseCase;
import com.elibrary.application.view.ReservationView;
import com.elibrary.domain.model.ReservationStatus;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.infrastructure.auth.MemberPrincipal;
import com.elibrary.interfaces.http.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1")
@Validated
@Tag(name = "Reservations", description = "Waiting list when all licenses are in use")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final CreateReservationUseCase createReservation;
    private final ListMyReservationsUseCase listMyReservations;
    private final CancelReservationUseCase cancelReservation;

    public ReservationController(CreateReservationUseCase createReservation,
                                 ListMyReservationsUseCase listMyReservations,
                                 CancelReservationUseCase cancelReservation) {
        this.createReservation = createReservation;
        this.listMyReservations = listMyReservations;
        this.cancelReservation = cancelReservation;
    }

    @PostMapping(path = "/books/{bookId}/reservations", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Join the waiting list",
            description = "Only allowed when no license is free (409 BOOK_AVAILABLE otherwise). When a copy "
                    + "is returned, the oldest waiter becomes READY and holds a license for 48 hours.")
    public ResponseEntity<ReservationView> reserve(@CurrentMember MemberPrincipal member,
                                                   @PathVariable String bookId) {
        ReservationView reservation = createReservation.execute(member.id(), bookId);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + reservation.id())).body(reservation);
    }

    @GetMapping(path = "/me/reservations", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List my reservations",
            description = "status is optional: waiting, ready, fulfilled, cancelled or expired.")
    public PageResponse<ReservationView> myReservations(
            @CurrentMember MemberPrincipal member,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listMyReservations.execute(member.id(),
                ReservationStatus.parseOrNull(status), new PageQuery(page, size)));
    }

    @DeleteMapping(path = "/reservations/{reservationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cancel a reservation",
            description = "A READY hold is released back to the pool and offered to the next waiter.")
    public ReservationView cancel(@CurrentMember MemberPrincipal member,
                                  @PathVariable String reservationId) {
        return cancelReservation.execute(member.id(), reservationId);
    }
}
