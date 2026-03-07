package com.edutech.psych.application.exception;

import java.util.UUID;

public class PsychProfileNotFoundException extends PsychException {

    public PsychProfileNotFoundException(UUID id) {
        super("Psych profile not found: " + id);
    }
}
