package com.edutech.aimentor.api;

import com.edutech.aimentor.application.exception.AiMentorException;
import com.edutech.aimentor.application.exception.DoubtNotFoundException;
import com.edutech.aimentor.application.exception.StudyPlanNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE_URI = "https://api.edutech.com/problems/ai-mentor";

    @ExceptionHandler(StudyPlanNotFoundException.class)
    public ProblemDetail handleStudyPlanNotFoundException(StudyPlanNotFoundException ex) {
        log.warn("Study plan not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Study Plan Not Found");
        problem.setType(URI.create(PROBLEM_BASE_URI + "/study-plan-not-found"));
        return problem;
    }

    @ExceptionHandler(DoubtNotFoundException.class)
    public ProblemDetail handleDoubtNotFoundException(DoubtNotFoundException ex) {
        log.warn("Doubt ticket not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Doubt Ticket Not Found");
        problem.setType(URI.create(PROBLEM_BASE_URI + "/doubt-not-found"));
        return problem;
    }

    @ExceptionHandler(AiMentorException.class)
    public ProblemDetail handleAiMentorException(AiMentorException ex) {
        log.error("AI mentor service error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("AI Mentor Service Error");
        problem.setType(URI.create(PROBLEM_BASE_URI + "/internal-error"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(PROBLEM_BASE_URI + "/validation-error"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request");
        problem.setType(URI.create(PROBLEM_BASE_URI + "/invalid-request"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(PROBLEM_BASE_URI + "/unexpected-error"));
        return problem;
    }
}
