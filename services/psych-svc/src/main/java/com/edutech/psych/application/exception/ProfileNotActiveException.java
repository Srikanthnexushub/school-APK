package com.edutech.psych.application.exception;

import java.util.UUID;

public class ProfileNotActiveException extends PsychException {

    public ProfileNotActiveException(UUID id) {
        super("Profile is not ACTIVE: " + id);
    }
}
