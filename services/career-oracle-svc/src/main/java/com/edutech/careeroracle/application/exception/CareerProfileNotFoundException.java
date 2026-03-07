package com.edutech.careeroracle.application.exception;

import java.util.UUID;

public class CareerProfileNotFoundException extends CareerOracleException {

    public CareerProfileNotFoundException(UUID id) {
        super("Career profile not found with id: " + id);
    }

    public CareerProfileNotFoundException(String field, UUID value) {
        super("Career profile not found with " + field + ": " + value);
    }
}
