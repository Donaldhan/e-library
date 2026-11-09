package com.elibrary.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration HOLD = Duration.ofHours(48);

    @Test
    void startsWaitingWithoutHoldDeadline() {
        Reservation reservation = Reservation.waiting("res-1", "book-1", "member-1", T0);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.WAITING);
        assertThat(reservation.getExpiresAt()).isNull();
        assertThat(reservation.isHoldExpiredAt(T0.plus(1000, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    void promotionStartsHoldWindow() {
        Reservation reservation = Reservation.waiting("res-1", "book-1", "member-1", T0);
        reservation.markReady(T0, HOLD);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(reservation.getExpiresAt()).isEqualTo(T0.plus(HOLD));
        assertThat(reservation.isHoldExpiredAt(T0.plus(HOLD))).isTrue();
        assertThat(reservation.isHoldExpiredAt(T0.plus(HOLD).minusSeconds(1))).isFalse();
    }

    @Test
    void expiryRequiresReadyState() {
        Reservation reservation = Reservation.waiting("res-1", "book-1", "member-1", T0);

        assertThatThrownBy(() -> reservation.expire(T0)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fulfillmentRequiresReadyHold() {
        Reservation reservation = Reservation.waiting("res-1", "book-1", "member-1", T0);

        assertThatThrownBy(() -> reservation.fulfill(T0)).isInstanceOf(IllegalStateException.class);
        reservation.markReady(T0, HOLD);
        reservation.fulfill(T0.plusSeconds(10));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
    }

    @Test
    void cancelIsOnlyPossibleWhileActive() {
        Reservation reservation = Reservation.waiting("res-1", "book-1", "member-1", T0);
        reservation.markReady(T0, HOLD);
        reservation.cancel(T0.plusSeconds(1));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThatThrownBy(() -> reservation.cancel(T0.plusSeconds(2))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void parsesFiltersLeniently() {
        assertThat(ReservationStatus.parseOrNull(null)).isNull();
        assertThat(ReservationStatus.parseOrNull(" ")).isNull();
        assertThat(ReservationStatus.parseOrNull("ready")).isEqualTo(ReservationStatus.READY);
        assertThatThrownBy(() -> ReservationStatus.parseOrNull("nope")).hasMessageContaining("Unsupported");
    }
}
