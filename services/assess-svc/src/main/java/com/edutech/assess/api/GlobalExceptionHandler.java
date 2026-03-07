// src/main/java/com/edutech/assess/api/GlobalExceptionHandler.java
package com.edutech.assess.api;

import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.DuplicateEnrollmentException;
import com.edutech.assess.application.exception.EnrollmentNotFoundException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.application.exception.ExamNotPublishedException;
import com.edutech.assess.application.exception.MaxAttemptsExceededException;
import com.edutech.assess.application.exception.QuestionNotFoundException;
import com.edutech.assess.application.exception.SubmissionAlreadySubmittedException;
import com.edutech.assess.application.exception.SubmissionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://edutech.com/problems/";

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(PROBLEM_BASE_URI + type));
        pd.setTitle(type);
        return pd;
    }

    @ExceptionHandler(ExamNotFoundException.class)
    public ProblemDetail handleExamNotFound(ExamNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "exam-not-found", ex.getMessage());
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ProblemDetail handleQuestionNotFound(QuestionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "question-not-found", ex.getMessage());
    }

    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ProblemDetail handleEnrollmentNotFound(EnrollmentNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "enrollment-not-found", ex.getMessage());
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ProblemDetail handleSubmissionNotFound(SubmissionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "submission-not-found", ex.getMessage());
    }

    @ExceptionHandler(AssessAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AssessAccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "assess-access-denied", ex.getMessage());
    }

    @ExceptionHandler(ExamNotPublishedException.class)
    public ProblemDetail handleExamNotPublished(ExamNotPublishedException ex) {
        return problem(HttpStatus.CONFLICT, "exam-not-published", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEnrollmentException.class)
    public ProblemDetail handleDuplicateEnrollment(DuplicateEnrollmentException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-enrollment", ex.getMessage());
    }

    @ExceptionHandler(SubmissionAlreadySubmittedException.class)
    public ProblemDetail handleSubmissionAlreadySubmitted(SubmissionAlreadySubmittedException ex) {
        return problem(HttpStatus.CONFLICT, "submission-already-submitted", ex.getMessage());
    }

    @ExceptionHandler(MaxAttemptsExceededException.class)
    public ProblemDetail handleMaxAttemptsExceeded(MaxAttemptsExceededException ex) {
        return problem(HttpStatus.CONFLICT, "max-attempts-exceeded", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-state-transition", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "validation-error", detail);
    }
}
