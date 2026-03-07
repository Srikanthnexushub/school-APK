package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.application.exception.MentorNotFoundException;
import com.edutech.mentorsvc.application.exception.MentorSessionNotFoundException;
import com.edutech.mentorsvc.application.exception.MentorSvcException;
import com.edutech.mentorsvc.application.exception.SessionAlreadyBookedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MentorNotFoundException.class)
    public ProblemDetail handleMentorNotFound(MentorNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://edutech.com/problems/mentor-not-found"));
        problem.setTitle("Mentor Not Found");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    @ExceptionHandler(MentorSessionNotFoundException.class)
    public ProblemDetail handleSessionNotFound(MentorSessionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://edutech.com/problems/session-not-found"));
        problem.setTitle("Mentor Session Not Found");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    @ExceptionHandler(SessionAlreadyBookedException.class)
    public ProblemDetail handleSessionAlreadyBooked(SessionAlreadyBookedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://edutech.com/problems/session-already-booked"));
        problem.setTitle("Session Already Booked");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    @ExceptionHandler(MentorSvcException.class)
    public ProblemDetail handleMentorSvcException(MentorSvcException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://edutech.com/problems/mentor-svc-error"));
        problem.setTitle("Mentor Service Error");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation failed for one or more fields.");
        problem.setType(URI.create("https://edutech.com/problems/validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("fieldErrors", fieldErrors);
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setType(URI.create("https://edutech.com/problems/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }
}
