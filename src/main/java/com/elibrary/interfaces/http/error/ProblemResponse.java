package com.elibrary.interfaces.http.error;

import com.elibrary.interfaces.http.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

/**
 * RFC 7807 style error body. {@code code} is stable and machine-readable; {@code detail} is for
 * humans and may change. {@code requestId} makes support conversations practical.
 */
public record ProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String requestId,
        List<FieldViolation> errors) {

    public static ProblemResponse of(HttpStatus status, String code, String detail,
                                     HttpServletRequest request, List<FieldViolation> errors) {
        return new ProblemResponse(
                "https://elibrary.local/errors/" + code.toLowerCase(Locale.ROOT).replace('_', '-'),
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                (String) request.getAttribute(RequestIdFilter.ATTRIBUTE),
                errors);
    }
}
