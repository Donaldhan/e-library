package com.elibrary.domain.exception;

/**
 * Base type for expected, business-level failures. The HTTP adapter maps the concrete subtype to
 * a status code and turns {@link #code()} into a stable field in the response body.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
