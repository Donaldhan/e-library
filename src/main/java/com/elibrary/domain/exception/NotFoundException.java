package com.elibrary.domain.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }

    public static NotFoundException book(String bookId) {
        return new NotFoundException(ErrorCode.BOOK_NOT_FOUND, "Book " + bookId + " was not found");
    }

    public static NotFoundException loan(String loanId) {
        return new NotFoundException(ErrorCode.LOAN_NOT_FOUND, "Loan " + loanId + " was not found");
    }

    public static NotFoundException reservation(String reservationId) {
        return new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                "Reservation " + reservationId + " was not found");
    }
}
