package com.elibrary.domain.exception;

/** A malformed or unsupported request parameter that Bean Validation cannot express. */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
