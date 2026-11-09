package com.elibrary.domain.service;

import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Member;
import com.elibrary.domain.model.MembershipType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LendingPolicyTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final Member member = Member.create("member-1", "Alice", "alice@example.com",
            MembershipType.BASIC, 2, "token", NOW);

    @Test
    void allowsBorrowWhenRulesAreSatisfied() {
        assertThatCode(() -> LendingPolicy.ensureMayBorrow(member, 1, false)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSecondActiveLoanOfSameTitle() {
        assertThatThrownBy(() -> LendingPolicy.ensureMayBorrow(member, 0, true))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).code())
                        .isEqualTo(ErrorCode.DUPLICATE_LOAN));
    }

    @Test
    void rejectsBorrowAtConcurrentLoanLimit() {
        assertThatThrownBy(() -> LendingPolicy.ensureMayBorrow(member, 2, false))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).code())
                        .isEqualTo(ErrorCode.LOAN_LIMIT_REACHED));
    }
}
