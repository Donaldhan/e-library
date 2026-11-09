package com.elibrary.domain.exception;

public class ConflictException extends DomainException {

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
