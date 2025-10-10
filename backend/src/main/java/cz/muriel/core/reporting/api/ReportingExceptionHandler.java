package cz.muriel.core.reporting.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Reporting module.
 */
@Slf4j
@RestControllerAdvice(basePackages = "cz.muriel.core.reporting.api")
public class ReportingExceptionHandler {

    /**
     * Handle validation errors from @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        detail.setProperty("errors", errors);
        return detail;
    }

    /**
     * Handle constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            errors.put(path, violation.getMessage());
        }

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        detail.setProperty("errors", errors);
        return detail;
    }

    /**
     * Handle illegal arguments (e.g., invalid query).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
    }

    /**
     * Handle access denied.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Access denied"
        );
    }

    /**
     * Handle Cube.js client errors (4xx).
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ProblemDetail handleCubeClientError(HttpClientErrorException ex) {
        log.error("Cube.js client error: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Query execution failed: " + ex.getResponseBodyAsString()
        );
    }

    /**
     * Handle Cube.js server errors (5xx).
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ProblemDetail handleCubeServerError(HttpServerErrorException ex) {
        log.error("Cube.js server error: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "Upstream service error - please retry later"
        );
    }

    /**
     * Handle generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
    }
}
