package com.elibrary.interfaces.http.error;

import com.elibrary.domain.exception.ConflictException;
import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ForbiddenException;
import com.elibrary.domain.exception.InvalidRequestException;
import com.elibrary.domain.exception.NotFoundException;
import com.elibrary.domain.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Single translation point from internal failures to the public error contract. Unexpected
 * exceptions are logged with the request id and never leak stack traces or SQL to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ex, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ex, request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemResponse> handleDomain(DomainException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request body contains invalid fields", request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemResponse> handleParameterValidation(ConstraintViolationException ex,
                                                                    HttpServletRequest request) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request contains invalid parameters", request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Malformed request body", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Parameter '" + ex.getName() + "' has an invalid value", request, null);
    }

    /** Database-level guards (unique indexes, CHECK constraints) caught at the boundary. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemResponse> handleIntegrityViolation(DataIntegrityViolationException ex,
                                                                   HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "CONFLICT",
                "The request conflicts with the current state of the resource", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request, null);
    }

    private static ResponseEntity<ProblemResponse> problem(HttpStatus status, DomainException ex,
                                                           HttpServletRequest request) {
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("Domain error mapped to 500: {}", ex.getMessage(), ex);
        }
        return problem(status, ex.code().name(), ex.getMessage(), request, null);
    }

    private static ResponseEntity<ProblemResponse> problem(HttpStatus status, String code, String detail,
                                                           HttpServletRequest request,
                                                           List<FieldViolation> errors) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemResponse.of(status, code, detail, request, errors));
    }
}
