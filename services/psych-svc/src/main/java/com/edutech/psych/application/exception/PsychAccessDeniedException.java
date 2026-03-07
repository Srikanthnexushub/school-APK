package com.edutech.psych.application.exception;

public class PsychAccessDeniedException extends PsychException {

    public PsychAccessDeniedException() {
        super("Access denied to psych resource");
    }
}
