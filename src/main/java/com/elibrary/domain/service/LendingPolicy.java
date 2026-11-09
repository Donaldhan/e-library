package com.elibrary.domain.service;

import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Member;

/**
 * Pure lending rules, kept free of persistence and transport concerns so they can be unit tested
 * and reasoned about in one place.
 */
public final class LendingPolicy {

    private LendingPolicy() {
    }

    public static void ensureMayBorrow(Member member, long activeLoanCount, boolean alreadyBorrowingTitle) {
        if (alreadyBorrowingTitle) {
            throw new ConflictException(ErrorCode.DUPLICATE_LOAN,
                    "Member " + member.getId() + " already has an active loan for this title");
        }
        if (activeLoanCount >= member.getMaxConcurrentLoans()) {
            throw new ConflictException(ErrorCode.LOAN_LIMIT_REACHED,
                    "Member " + member.getId() + " has reached the limit of "
                            + member.getMaxConcurrentLoans() + " concurrent loans");
        }
    }
}
