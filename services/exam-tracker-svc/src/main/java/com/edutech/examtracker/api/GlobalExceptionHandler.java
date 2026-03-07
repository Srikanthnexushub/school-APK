package com.edutech.examtracker.api;

import com.edutech.examtracker.application.exception.DuplicateEnrollmentException;
import com.edutech.examtracker.application.exception.EnrollmentNotFoundException;
import com.edutech.examtracker.application.exception.ExamTrackerException;
import com.edutech.examtracker.application.exception.ModuleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://edutech.com/problems/";

    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ProblemDetail handleEnrollmentNotFound(EnrollmentNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "enrollment-not-found", ex.getMessage());
    }

    @ExceptionHandler(ModuleNotFoundException.class)
    public ProblemDetail handleModuleNotFound(ModuleNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "module-not-found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEnrollmentException.class)
    public ProblemDetail handleDuplicateEnrollment(DuplicateEnrollmentException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-enrollment", ex.getMessage());
    }

    @ExceptionHandler(ExamTrackerException.class)
    public ProblemDetail handleExamTrackerException(ExamTrackerException ex) {
        return problem(HttpStatus.BAD_REQUEST, "exam-tracker-error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(BASE_URI + "validation-error"));
        pd.setTitle("Validation failed");
        pd.setDetail(ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "; " + b));
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-state-transition", ex.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(BASE_URI + type));
        pd.setTitle(type.replace("-", " "));
        pd.setDetail(detail);
        return pd;
    }
}
