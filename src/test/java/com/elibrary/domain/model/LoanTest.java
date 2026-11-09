package com.elibrary.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void derivesDueDateFromLoanPeriod() {
        Loan loan = Loan.create("loan-1", "book-1", "member-1", T0, 14);

        assertThat(loan.getDueAt()).isEqualTo(T0.plus(14, ChronoUnit.DAYS));
        assertThat(loan.statusAt(T0)).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.isActive()).isTrue();
    }

    @Test
    void becomesOverdueStrictlyAfterDueDate() {
        Loan loan = Loan.create("loan-1", "book-1", "member-1", T0, 14);

        assertThat(loan.isOverdue(loan.getDueAt())).isFalse();
        assertThat(loan.isOverdue(loan.getDueAt().plusMillis(1))).isTrue();
        assertThat(loan.statusAt(loan.getDueAt().plus(1, ChronoUnit.DAYS))).isEqualTo(LoanStatus.OVERDUE);
    }

    @Test
    void returnedLoanIsNeverOverdue() {
        Loan loan = Loan.create("loan-1", "book-1", "member-1", T0, 14);
        loan.markReturned(T0.plus(2, ChronoUnit.DAYS));

        assertThat(loan.statusAt(T0.plus(30, ChronoUnit.DAYS))).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.isActive()).isFalse();
    }

    @Test
    void rejectsDoubleReturn() {
        Loan loan = Loan.create("loan-1", "book-1", "member-1", T0, 14);
        loan.markReturned(T0.plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> loan.markReturned(T0.plus(2, ChronoUnit.DAYS)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsReturnBeforeBorrow() {
        Loan loan = Loan.create("loan-1", "book-1", "member-1", T0, 14);

        assertThatThrownBy(() -> loan.markReturned(T0.minus(1, ChronoUnit.DAYS)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveLoanPeriod() {
        assertThatThrownBy(() -> Loan.create("loan-1", "book-1", "member-1", T0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
