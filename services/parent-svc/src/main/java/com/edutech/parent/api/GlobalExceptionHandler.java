// src/main/java/com/edutech/parent/api/GlobalExceptionHandler.java
package com.edutech.parent.api;

import com.edutech.parent.application.exception.ConversationNotFoundException;
import com.edutech.parent.application.exception.DuplicateStudentLinkException;
import com.edutech.parent.application.exception.FeePaymentNotFoundException;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.application.exception.ParentProfileNotFoundException;
import com.edutech.parent.application.exception.StudentLinkNotFoundException;
import com.edutech.parent.application.exception.TooManyChildrenException;
import com.edutech.parent.application.exception.TooManyParentsException;
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

    private static final String BASE_URI = "https://edutech.com/problems/";

    @ExceptionHandler(ParentProfileNotFoundException.class)
    public ProblemDetail handleParentProfileNotFound(ParentProfileNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "parent-profile-not-found"));
        pd.setTitle("Parent Profile Not Found");
        return pd;
    }

    @ExceptionHandler(StudentLinkNotFoundException.class)
    public ProblemDetail handleStudentLinkNotFound(StudentLinkNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "student-link-not-found"));
        pd.setTitle("Student Link Not Found");
        return pd;
    }

    @ExceptionHandler(DuplicateStudentLinkException.class)
    public ProblemDetail handleDuplicateStudentLink(DuplicateStudentLinkException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "duplicate-student-link"));
        pd.setTitle("Duplicate Student Link");
        return pd;
    }

    @ExceptionHandler(ParentAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(ParentAccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "parent-access-denied"));
        pd.setTitle("Access Denied");
        return pd;
    }

    @ExceptionHandler(FeePaymentNotFoundException.class)
    public ProblemDetail handleFeePaymentNotFound(FeePaymentNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "fee-payment-not-found"));
        pd.setTitle("Fee Payment Not Found");
        return pd;
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ProblemDetail handleConversationNotFound(ConversationNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "conversation-not-found"));
        pd.setTitle("Copilot Conversation Not Found");
        return pd;
    }

    @ExceptionHandler(TooManyChildrenException.class)
    public ProblemDetail handleTooManyChildren(TooManyChildrenException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "too-many-children"));
        pd.setTitle("Child Limit Reached");
        return pd;
    }

    @ExceptionHandler(TooManyParentsException.class)
    public ProblemDetail handleTooManyParents(TooManyParentsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create(BASE_URI + "too-many-parents"));
        pd.setTitle("Parent Limit Reached");
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
}
