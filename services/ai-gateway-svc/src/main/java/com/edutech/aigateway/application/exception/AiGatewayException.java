package com.edutech.aigateway.application.exception;

public abstract class AiGatewayException extends RuntimeException {

    public AiGatewayException(String message) {
        super(message);
    }
}
