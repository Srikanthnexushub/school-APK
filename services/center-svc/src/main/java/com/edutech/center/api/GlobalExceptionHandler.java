// src/main/java/com/edutech/center/api/GlobalExceptionHandler.java
package com.edutech.center.api;

import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.DuplicateCenterCodeException;
import com.edutech.center.application.exception.ScheduleConflictException;
import com.edutech.center.application.exception.TeacherAlreadyAssignedException;
import com.edutech.center.application.exception.TeacherNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://edutech.com/problems/";

    @ExceptionHandler(CenterNotFoundException.class)
    public ProblemDetail handleCenterNotFound(CenterNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "center-not-found", ex.getMessage());
    }

    @ExceptionHandler(BatchNotFoundException.class)
    public ProblemDetail handleBatchNotFound(BatchNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "batch-not-found", ex.getMessage());
    }

    @ExceptionHandler(TeacherNotFoundException.class)
    public ProblemDetail handleTeacherNotFound(TeacherNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "teacher-not-found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateCenterCodeException.class)
    public ProblemDetail handleDuplicateCode(DuplicateCenterCodeException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-center-code", ex.getMessage());
    }

    @ExceptionHandler(TeacherAlreadyAssignedException.class)
    public ProblemDetail handleTeacherAlreadyAssigned(TeacherAlreadyAssignedException ex) {
        return problem(HttpStatus.CONFLICT, "teacher-already-assigned", ex.getMessage());
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ProblemDetail handleScheduleConflict(ScheduleConflictException ex) {
        return problem(HttpStatus.CONFLICT, "schedule-conflict", ex.getMessage());
    }

    @ExceptionHandler(CenterAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(CenterAccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "access-denied", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-state-transition", ex.getMessage());
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

    @ExceptionHandler(CenterException.class)
    public ProblemDetail handleCenterException(CenterException ex) {
        return problem(HttpStatus.BAD_REQUEST, "center-error", ex.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(BASE_URI + type));
        pd.setTitle(type.replace("-", " "));
        pd.setDetail(detail);
        return pd;
    }
}
