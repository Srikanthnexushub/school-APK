// src/main/java/com/edutech/auth/api/GlobalExceptionHandler.java
package com.edutech.auth.api;

import com.edutech.auth.application.exception.AccountLockedException;
import com.edutech.auth.application.exception.AccountNotVerifiedException;
import com.edutech.auth.application.exception.CaptchaVerificationException;
import com.edutech.auth.application.exception.EmailAlreadyExistsException;
import com.edutech.auth.application.exception.InvalidCredentialsException;
import com.edutech.auth.application.exception.InvalidTokenException;
import com.edutech.auth.application.exception.OtpExpiredException;
import com.edutech.auth.application.exception.OtpMaxAttemptsExceededException;
import com.edutech.auth.application.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler — all responses follow RFC 7807 ProblemDetail.
 * No stack traces are leaked to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_TYPE_BASE = "https://edutech.com/problems/";

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", ex.getMessage());
    }

    @ExceptionHandler(CaptchaVerificationException.class)
    public ProblemDetail handleCaptcha(CaptchaVerificationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "captcha-failed", ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, "email-already-exists", ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        return problem(HttpStatus.LOCKED, "account-locked", ex.getMessage());
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ProblemDetail handleNotVerified(AccountNotVerifiedException ex) {
        return problem(HttpStatus.FORBIDDEN, "account-not-verified", ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-token", ex.getMessage());
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ProblemDetail handleOtpExpired(OtpExpiredException ex) {
        return problem(HttpStatus.GONE, "otp-expired", ex.getMessage());
    }

    @ExceptionHandler(OtpMaxAttemptsExceededException.class)
    public ProblemDetail handleOtpMaxAttempts(OtpMaxAttemptsExceededException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "otp-max-attempts", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "user-not-found", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                (a, b) -> a
            ));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        pd.setTitle("Validation Error");
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
            "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(PROBLEM_TYPE_BASE + type));
        pd.setTitle(toTitle(type));
        return pd;
    }

    private String toTitle(String type) {
        return type.replace("-", " ")
            .substring(0, 1).toUpperCase()
            + type.replace("-", " ").substring(1);
    }
}
