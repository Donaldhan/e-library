package com.elibrary.domain.exception;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
