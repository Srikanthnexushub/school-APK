package com.edutech.aigateway.api;

import com.edutech.aigateway.application.exception.AiGatewayException;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.application.exception.ModelNotFoundException;
import com.edutech.aigateway.application.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleRateLimitExceeded(RateLimitExceededException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        pd.setType(URI.create("https://edutech.com/problems/rate-limit-exceeded"));
        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(pd));
    }

    @ExceptionHandler(AiProviderException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleAiProviderException(AiProviderException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        pd.setType(URI.create("https://edutech.com/problems/ai-provider-error"));
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd));
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleModelNotFoundException(ModelNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setType(URI.create("https://edutech.com/problems/model-not-found"));
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd));
    }

    @ExceptionHandler(AiGatewayException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleAiGatewayException(AiGatewayException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        pd.setType(URI.create("https://edutech.com/problems/ai-gateway-error"));
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidationException(WebExchangeBindException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setType(URI.create("https://edutech.com/problems/validation-error"));
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleException(Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        pd.setType(URI.create("https://edutech.com/problems/internal-error"));
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd));
    }
}
