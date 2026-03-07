package com.edutech.psych.application.exception;

import java.util.UUID;

public class CareerMappingNotFoundException extends PsychException {

    public CareerMappingNotFoundException(UUID id) {
        super("Career mapping not found: " + id);
    }
}
