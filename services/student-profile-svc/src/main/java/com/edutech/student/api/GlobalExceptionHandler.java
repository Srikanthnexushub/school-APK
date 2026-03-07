package com.edutech.student.api;

import com.edutech.student.application.exception.AcademicRecordNotFoundException;
import com.edutech.student.application.exception.DuplicateStudentException;
import com.edutech.student.application.exception.StudentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://edupath.edutech.com/problems/";

    @ExceptionHandler(StudentNotFoundException.class)
    public ProblemDetail handleStudentNotFound(StudentNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "student-not-found"));
        pd.setTitle("Student Not Found");
        return pd;
    }

    @ExceptionHandler(DuplicateStudentException.class)
    public ProblemDetail handleDuplicateStudent(DuplicateStudentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "duplicate-student"));
        pd.setTitle("Duplicate Student");
        return pd;
    }

    @ExceptionHandler(AcademicRecordNotFoundException.class)
    public ProblemDetail handleAcademicRecordNotFound(AcademicRecordNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "academic-record-not-found"));
        pd.setTitle("Academic Record Not Found");
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "invalid-state-transition"));
        pd.setTitle("Invalid State Transition");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, detail);
        pd.setType(URI.create(BASE_URI + "validation-error"));
        pd.setTitle("Validation Error");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setType(URI.create(BASE_URI + "internal-error"));
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
