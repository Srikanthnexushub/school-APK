package com.edutech.performance.api;

import com.edutech.performance.application.exception.ReadinessScoreNotFoundException;
import com.edutech.performance.application.exception.WeakAreaNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URI = "https://edutech.com/problems/";

    @ExceptionHandler(ReadinessScoreNotFoundException.class)
    public ProblemDetail handleReadinessScoreNotFound(ReadinessScoreNotFoundException ex) {
        log.warn("Readiness score not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "readiness-score-not-found"));
        detail.setTitle("Readiness Score Not Found");
        return detail;
    }

    @ExceptionHandler(WeakAreaNotFoundException.class)
    public ProblemDetail handleWeakAreaNotFound(WeakAreaNotFoundException ex) {
        log.warn("Weak area not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "weak-area-not-found"));
        detail.setTitle("Weak Area Not Found");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map((FieldError fe) -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value"
                ))
                .collect(Collectors.toList());

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed. Check the 'fieldErrors' property for details."
        );
        detail.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        detail.setTitle("Validation Error");
        detail.setProperty("fieldErrors", fieldErrors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        detail.setType(URI.create(PROBLEM_BASE_URI + "internal-error"));
        detail.setTitle("Internal Server Error");
        return detail;
    }
}
