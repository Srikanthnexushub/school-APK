package com.edutech.careeroracle.application.exception;

public class CareerOracleException extends RuntimeException {

    public CareerOracleException(String message) {
        super(message);
    }

    public CareerOracleException(String message, Throwable cause) {
        super(message, cause);
    }
}
