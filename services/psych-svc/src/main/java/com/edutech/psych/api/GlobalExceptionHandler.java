package com.edutech.psych.api;

import com.edutech.psych.application.exception.CareerMappingNotFoundException;
import com.edutech.psych.application.exception.ProfileNotActiveException;
import com.edutech.psych.application.exception.PsychAccessDeniedException;
import com.edutech.psych.application.exception.PsychProfileNotFoundException;
import com.edutech.psych.application.exception.SessionAlreadyCompletedException;
import com.edutech.psych.application.exception.SessionHistoryNotFoundException;
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

    @ExceptionHandler(PsychProfileNotFoundException.class)
    public ProblemDetail handlePsychProfileNotFound(PsychProfileNotFoundException ex) {
        log.warn("Psych profile not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "psych-profile-not-found"));
        detail.setTitle("Psych Profile Not Found");
        return detail;
    }

    @ExceptionHandler(SessionHistoryNotFoundException.class)
    public ProblemDetail handleSessionHistoryNotFound(SessionHistoryNotFoundException ex) {
        log.warn("Session not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "session-not-found"));
        detail.setTitle("Session Not Found");
        return detail;
    }

    @ExceptionHandler(CareerMappingNotFoundException.class)
    public ProblemDetail handleCareerMappingNotFound(CareerMappingNotFoundException ex) {
        log.warn("Career mapping not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "career-mapping-not-found"));
        detail.setTitle("Career Mapping Not Found");
        return detail;
    }

    @ExceptionHandler(PsychAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(PsychAccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "access-denied"));
        detail.setTitle("Access Denied");
        return detail;
    }

    @ExceptionHandler(SessionAlreadyCompletedException.class)
    public ProblemDetail handleSessionAlreadyCompleted(SessionAlreadyCompletedException ex) {
        log.warn("Session already completed: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "session-already-completed"));
        detail.setTitle("Session Already Completed");
        return detail;
    }

    @ExceptionHandler(ProfileNotActiveException.class)
    public ProblemDetail handleProfileNotActive(ProfileNotActiveException ex) {
        log.warn("Profile not active: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setType(URI.create(PROBLEM_BASE_URI + "profile-not-active"));
        detail.setTitle("Profile Not Active");
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
