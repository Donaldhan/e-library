package com.elibrary.domain.exception;

public class ForbiddenException extends DomainException {

    public ForbiddenException(ErrorCode code, String message) {
        super(code, message);
    }
}
