package com.edutech.aigateway.application.exception;

public class AiProviderException extends AiGatewayException {

    public AiProviderException(String message) {
        super("AI provider error: " + message);
    }
}
